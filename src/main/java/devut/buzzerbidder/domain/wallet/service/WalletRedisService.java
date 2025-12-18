package devut.buzzerbidder.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WalletRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    /* ==================== Redis Key 규칙 ==================== */

    // 경매 진행 중 잔액은 auction 네임스페이스로 통일
    private static final String SESSION_KEY_PREFIX = "auction:session:";  // value = roomId
    private static final String BAL_KEY_PREFIX = "auction:bizz:";         // value = balance
    private static final String VER_KEY_PREFIX = "auction:bizzver:";      // value = version(sequence)

    // 잔액 변경/flush 이벤트를 쌓는 Redis Stream
    private static final String EVENT_STREAM = "auction:bizz:events";

    // TTL은 heartbeat(주기적으로 연결이 살아있는지 확인하는 신호) 또는 잔액 변경 때마다 연장
    private static final Duration TTL = Duration.ofSeconds(30);

    // Lua에서 'Redis에 키가 없는 상태'를 표현하기 위한 값
    private static final long MISS = -2L;

    // Lua에서 '잔액 부족'을 표현하기 위한 값
    private static final long INSUFFICIENT = -1L;

    // 이벤트 스트림의 최대 기록 개수
    private static final Long STREAM_MAXLEN = 100_000L;


    /* ==================== Script 정의 ==================== */

    // 1) 세션 획득 + (balance/version 초기화) + stream 로그
    private final DefaultRedisScript<Long> acquireAndInitScript = buildAcquireAndInitScript();

    // 2) balance 키가 존재하면 원자적으로 증감 + version 증가 + TTL 연장 + stream 로그
    private final DefaultRedisScript<List<Long>> changeIfPresentScript = buildChangeIfPresentScript();

    // 3) flush: 최종 잔액/버전/roomId를 얻고 키 삭제 + stream 로그
    private final DefaultRedisScript<List<String>> flushAndClearScript = buildFlushAndClearScript();

    /* ==================== 결과 DTO ==================== */

    /**
     * Redis에 올라와 있을 때만 잔액 변경
     * hit은 처리 성공 여부, hit=false면 Redis에 없음
     */
    public record RedisBizzChangeResult(
            boolean hit,
            Long before,
            Long after,
            Long version
    ) {}

    /**
     * flush 결과
     * hit=true면 Redis에서 최종 값을 얻어 DB에 반영할 수 있음
     */
    public record RedisFlushResult(
            boolean hit,
            String roomId,
            Long finalBalance,
            Long version
    ) {}

    /* ==================== public ==================== */

    /**
     * 세션을 획득하고, 획득에 성공하면 잔액/버전 초기화
     *
     * - 성공 시: sessionKey, balKey, verKey 생성 + TTL 설정 + Stream에 INIT 이벤트 기록
     * - 실패 시: (이미 세션이 있다면) 아무 것도 변경하지 않고 false 반환
     */
    public boolean tryAcquireSessionAndInitBalance(Long userId, Long roomId, Long balanceFromDb, String traceId) {
        String sKey = SESSION_KEY_PREFIX + userId;
        String bKey = BAL_KEY_PREFIX + userId;
        String vKey = VER_KEY_PREFIX + userId;

        // result: 성공 시 1, 실패 시 0
        Long result = Objects.requireNonNull(stringRedisTemplate.execute(
                acquireAndInitScript,
                List.of(sKey, bKey, vKey),
                userId.toString(),
                roomId.toString(),
                balanceFromDb.toString(),
                String.valueOf(TTL.getSeconds()),
                EVENT_STREAM,
                STREAM_MAXLEN.toString(),
                traceId == null ? "" : traceId
                ),
                "Redis script가 null을 반환했습니다."
        );

        return result == 1L;
    }

    /**
     * Redis에 잔액이 '이미 올라와 있을 때만' 증감

     * - Redis에 bizz 키가 없으면 hit=false로 반환
     * - 잔액 부족이면 INSUFFICIENT로 처리
     */
    public RedisBizzChangeResult changeBalanceIfPresent(
            Long userId,
            Long amount,
            boolean isIncrease,
            String reason,
            String traceId
    ) {
        String sKey = SESSION_KEY_PREFIX + userId;
        String bKey = BAL_KEY_PREFIX + userId;
        String vKey = VER_KEY_PREFIX + userId;

        // result: [before, after, version]
        List<Long> result = executeLongList(
                changeIfPresentScript,
                List.of(bKey, vKey, sKey),
                userId.toString(),
                amount.toString(),
                isIncrease ? "1" : "0",
                reason == null ? "" : reason,
                traceId == null ? "" : traceId,
                String.valueOf(TTL.getSeconds()),
                EVENT_STREAM,
                STREAM_MAXLEN.toString()
        );
        if (result.size() != 3) {
            throw new IllegalStateException("Redis change 스크립트 반환 형식이 예상과 다릅니다. userId=" + userId + ", result=" + result);
        }

        Long before = result.get(0);
        Long after = result.get(1);
        Long ver = result.get(2);

        // Redis에 잔액 자체가 없으면 경매 중이 아님/세션 없음/만료됨 -> 호출자가 DB fallback 처리
        if (before.equals(MISS)) {
            return new RedisBizzChangeResult(false, null, null, null);
        }

        // 잔액 부족이면 예외처리
        if (after.equals(INSUFFICIENT)) {
            throw new devut.buzzerbidder.global.exeption.BusinessException(
                    devut.buzzerbidder.global.exeption.ErrorCode.BIZZ_INSUFFICIENT_BALANCE
            );
        }

        return new RedisBizzChangeResult(true, before, after, ver);
    }

    /**
     * Redis에 잔액이 올라와 있으면 최종 값을 꺼내고, 관련 키를 삭제
     *
     * - hit=false면 이미 만료/삭제된 상태 -> 호출자가 “DB flush 불가” 케이스로 처리
     * - hit=true면 finalBalance를 DB에 저장하고 종료하면 됨
     */
    public RedisFlushResult flushBalanceAndClearSession(Long userId, String traceId) {
        String sKey = SESSION_KEY_PREFIX + userId;
        String bKey = BAL_KEY_PREFIX + userId;
        String vKey = VER_KEY_PREFIX + userId;

        // result: [hit, roomId, finalBalance, version]
        List<String> result = executeStringList(
                flushAndClearScript,
                List.of(sKey, bKey, vKey),
                userId.toString(),
                EVENT_STREAM,
                STREAM_MAXLEN.toString(),
                traceId == null ? "" : traceId
        );
        if (result.size() != 4) {
            throw new IllegalStateException("Redis flush 스크립트 반환 형식이 예상과 다릅니다. userId=" + userId + ", result: " + result);
        }


        boolean hit = "1".equals(result.get(0));
        if (!hit) {
            return new RedisFlushResult(false, null, null, null);
        }

        return new RedisFlushResult(
                true,
                result.get(1),
                Long.parseLong(result.get(2)),
                Long.parseLong(result.get(3))
        );
    }

    /** 세션/잔액/버전 키의 TTL을 연장 */
    public void extendTtl(Long userId) {
        stringRedisTemplate.expire(SESSION_KEY_PREFIX + userId, TTL);
        stringRedisTemplate.expire(BAL_KEY_PREFIX + userId, TTL);
        stringRedisTemplate.expire(VER_KEY_PREFIX + userId, TTL);
    }

    /* ==================== execute 헬퍼 (unchecked 경고를 한 곳에만 모음) ==================== */

    private List<Long> executeLongList(DefaultRedisScript<List<Long>> script, List<String> keys, String... args) {
        return Objects.requireNonNull(
                stringRedisTemplate.execute(script, keys, (Object[]) args),
                "Redis script가 null을 반환했습니다."
        );
    }

    private List<String> executeStringList(DefaultRedisScript<List<String>> script, List<String> keys, String... args) {
        return Objects.requireNonNull(
                stringRedisTemplate.execute(script, keys, (Object[]) args),
                "Redis script가 null을 반환했습니다."
        );
    }

    /* ==================== Lua Script 빌더 ==================== */

    private DefaultRedisScript<Long> buildAcquireAndInitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);

        script.setScriptText("""
            local sKey = KEYS[1]
            local bKey = KEYS[2]
            local vKey = KEYS[3]

            local userId = ARGV[1]
            local roomId = ARGV[2]
            local bal = ARGV[3]
            local ttl = tonumber(ARGV[4])
            local stream = ARGV[5]
            local maxlen = tonumber(ARGV[6])
            local traceId = ARGV[7]

            -- 세션이 이미 있으면 “획득 실패”
            if redis.call('EXISTS', sKey) == 1 then
              return 0
            end

            -- 세션 획득 + 초기화
            redis.call('SET', sKey, roomId, 'EX', ttl)
            redis.call('SET', bKey, bal, 'EX', ttl)
            redis.call('SET', vKey, '0', 'EX', ttl)

            -- INIT 이벤트 기록 (Kafka는 별도 워커가 Stream을 읽어 발행)
            redis.call('XADD', stream, 'MAXLEN', '~', maxlen, '*',
              'event', 'INIT',
              'userId', userId,
              'roomId', roomId,
              'balance', bal,
              'traceId', traceId
            )

            return 1
        """);

        return script;
    }

    private DefaultRedisScript<List<Long>> buildChangeIfPresentScript() {
        DefaultRedisScript<List<Long>> script = new DefaultRedisScript<>();

        @SuppressWarnings({"rawtypes", "unchecked"})
        Class<List<Long>> listClass = (Class) List.class;
        script.setResultType(listClass);

        script.setScriptText("""
            local balKey = KEYS[1]
            local verKey = KEYS[2]
            local sesKey = KEYS[3]

            local userId = ARGV[1]
            local amount = tonumber(ARGV[2])
            local isInc = tonumber(ARGV[3])
            local reason = ARGV[4]
            local traceId = ARGV[5]
            local ttl = tonumber(ARGV[6])
            local stream = ARGV[7]
            local maxlen = tonumber(ARGV[8])

            --세션이 없으면 MISS
            if redis.call('EXISTS', sesKey) == 0 then
              return { %d, %d, 0 }
            end

            local cur = redis.call('GET', balKey)
            if not cur then
              -- Redis에 잔액이 없다 = 경매 중이 아님(또는 만료됨)
              return { %d, %d, 0 }
            end
            cur = tonumber(cur)

            -- 감소인데 잔액 부족이면 실패
            if isInc == 0 and cur < amount then
              local ver = redis.call('GET', verKey) or "0"
              return { cur, %d, tonumber(ver) }
            end

            local newv
            if isInc == 1 then newv = cur + amount else newv = cur - amount end

            -- 잔액 반영 + 버전 증가
            redis.call('SET', balKey, newv)
            local ver = redis.call('INCR', verKey)

            -- TTL 연장(세션/잔액/버전 모두)
            redis.call('EXPIRE', balKey, ttl)
            redis.call('EXPIRE', verKey, ttl)
            redis.call('EXPIRE', sesKey, ttl)

            -- 이벤트 기록
            redis.call('XADD', stream, 'MAXLEN', '~', maxlen, '*',
              'event', 'CHANGE',
              'userId', userId,
              'before', tostring(cur),
              'after', tostring(newv),
              'amount', tostring(amount),
              'isIncrease', tostring(isInc),
              'version', tostring(ver),
              'reason', reason,
              'traceId', traceId
            )

            return { cur, newv, ver }
        """.formatted(MISS, MISS, MISS, MISS, INSUFFICIENT));

        return script;
    }

    private DefaultRedisScript<List<String>> buildFlushAndClearScript() {
        DefaultRedisScript<List<String>> script = new DefaultRedisScript<>();

        @SuppressWarnings({"rawtypes", "unchecked"})
        Class<List<String>> listClass = (Class) List.class;
        script.setResultType(listClass);

        script.setScriptText("""
            local sKey = KEYS[1]
            local bKey = KEYS[2]
            local vKey = KEYS[3]

            local userId = ARGV[1]
            local stream = ARGV[2]
            local maxlen = tonumber(ARGV[3])
            local traceId = ARGV[4]

            local bal = redis.call('GET', bKey)
            if not bal then
              return { '0', '', '0', '0' }
            end

            local ver = redis.call('GET', vKey) or "0"
            local roomId = redis.call('GET', sKey) or ""

            -- 정리: 관련 키 삭제
            redis.call('DEL', sKey)
            redis.call('DEL', bKey)
            redis.call('DEL', vKey)

            -- FLUSH 이벤트 기록
            redis.call('XADD', stream, 'MAXLEN', '~', maxlen, '*',
              'event', 'FLUSH_CLEAR',
              'userId', userId,
              'roomId', roomId,
              'finalBalance', bal,
              'version', ver,
              'traceId', traceId
            )

            return { '1', roomId, bal, ver }
        """);

        return script;
    }
}
