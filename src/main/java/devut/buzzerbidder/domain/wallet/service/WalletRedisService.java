package devut.buzzerbidder.domain.wallet.service;

import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    /* ==================== Redis Key 규칙 ==================== */

    // 경매 진행 중 잔액은 auction 네임스페이스로 통일
    private static final String SESSION_KEY_PREFIX = "auction:session:";  // value = roomId
    private static final String BAL_KEY_PREFIX = "auction:bizz:";         // value = balance
    private static final String VER_KEY_PREFIX = "auction:bizzver:";      // value = version(sequence)

    // 잔액 변경/flush 이벤트를 쌓는 Redis Stream
    private static final String EVENT_STREAM = "auction:bizz:events";

    // SESSION_TTL은 heartbeat(주기적으로 연결이 살아있는지 확인하는 신호) 또는 잔액 변경 때마다 연장
    private static final Duration SESSION_TTL = Duration.ofSeconds(35);
    private static final Duration BALANCE_TTL = Duration.ofMinutes(10); // 예시

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

    // 3) transfer: 두 유저 balance가 존재할 때만 원자 송금 + version 증가 + TTL 연장 + stream 로그
    private final DefaultRedisScript<List<Long>> transferIfPresentScript = buildTransferIfPresentScript();

    // 4) flush: 최종 잔액/버전/roomId를 얻고 키 삭제 + stream 로그
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
     * Redis에 올라와 있을 때만 송금(출금+입금) 수행
     * hit은 처리 성공 여부, hit=false면 Redis에 없음
     */
    public record RedisTransferResult(
            boolean hit,
            Long fromBefore,
            Long fromAfter,
            Long fromVersion,
            Long toBefore,
            Long toAfter,
            Long toVersion
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

    public Long getBizzBalance(Long userId) {
        String key = "auction:bizz:" + userId;
        String redisBizzStr = stringRedisTemplate.opsForValue().get(key);

        if(redisBizzStr != null && !redisBizzStr.isBlank()) {
            try {
                return Long.parseLong(redisBizzStr);
            } catch (NumberFormatException e) {
                log.error("Redis의 Bizz 값이 숫자가 아닙니다. userId={}, key={}, Redis result={}", userId, key, redisBizzStr);
                throw new BusinessException(ErrorCode.REDIS_INVALID_BIZZ_VALUE);
            }
        }

        return null;
    }

    /**
     * 세션을 획득하고, 획득에 성공하면 잔액/버전 초기화
     *
     * - 성공 시: sessionKey, balKey, verKey 생성 + TTL 설정 + Stream에 INIT 이벤트 기록
     * - 실패 시: (이미 세션이 있다면) 아무 것도 변경하지 않고 false 반환
     */
    @Timed(
            value = "buzzerbidder.redis.wallet",
            extraTags = {"op", "acquire"},
            histogram = true
    )
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
                        String.valueOf(SESSION_TTL.getSeconds()),
                        String.valueOf(BALANCE_TTL.getSeconds()),
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
    public RedisBizzChangeResult changeBizzIfPresent(
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
                String.valueOf(SESSION_TTL.getSeconds()),
                String.valueOf(BALANCE_TTL.getSeconds()),
                EVENT_STREAM,
                STREAM_MAXLEN.toString()
        );
        if (result.size() != 3) {
            log.error("Redis change 스크립트 반환 형식이 예상과 다릅니다. userId={}, result={}", userId, result);
            throw new BusinessException(ErrorCode.UNEXPECTED_REDIS_SCRIPT_RETURN);
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
     * Redis에 잔액이 '이미 올라와 있을 때만' 송금(출금+입금)을 단일 연산으로 처리
     *
     * - Redis에 키가 없거나 세션이 없으면 hit=false
     * - 잔액 부족이면 INSUFFICIENT로 처리(예외)
     * - 성공하면 from/to 잔액 변경 + 각자 version INCR + TTL 연장 + Stream TRANSFER 이벤트 기록
     */
    public RedisTransferResult transferBizzIfPresent(
            Long fromUserId,
            Long toUserId,
            Long amount,
            String reason,
            String traceId
    ) {
        if (fromUserId == null || toUserId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (Objects.equals(fromUserId, toUserId)) {
            throw new BusinessException(ErrorCode.INVALID_TRANSFER);
        }
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_WALLET_AMOUNT);
        }

        String fromSKey = SESSION_KEY_PREFIX + fromUserId;
        String fromBKey = BAL_KEY_PREFIX + fromUserId;
        String fromVKey = VER_KEY_PREFIX + fromUserId;

        String toSKey = SESSION_KEY_PREFIX + toUserId;
        String toBKey = BAL_KEY_PREFIX + toUserId;
        String toVKey = VER_KEY_PREFIX + toUserId;

        // result: [fromBefore, fromAfter, fromVer, toBefore, toAfter, toVer]
        List<Long> result = executeLongList(
                transferIfPresentScript,
                List.of(fromBKey, fromVKey, fromSKey, toBKey, toVKey, toSKey),
                fromUserId.toString(),
                toUserId.toString(),
                amount.toString(),
                reason == null ? "" : reason,
                traceId == null ? "" : traceId,
                String.valueOf(SESSION_TTL.getSeconds()),
                String.valueOf(BALANCE_TTL.getSeconds()),
                EVENT_STREAM,
                STREAM_MAXLEN.toString()
        );

        if (result.size() != 6) {
            log.error("Redis transfer 스크립트 반환 형식이 예상과 다릅니다. from={}, to={}, result={}",
                    fromUserId, toUserId, result);
            throw new BusinessException(ErrorCode.UNEXPECTED_REDIS_SCRIPT_RETURN);
        }

        Long fromBefore = result.get(0);
        Long fromAfter = result.get(1);
        Long fromVer = result.get(2);
        Long toBefore = result.get(3);
        Long toAfter = result.get(4);
        Long toVer = result.get(5);

        // MISS면 Redis에 필요한 상태가 없음 -> 호출자가 DB fallback
        if (fromBefore.equals(MISS) || toBefore.equals(MISS)) {
            return new RedisTransferResult(false, null, null, null, null, null, null);
        }

        // 잔액 부족이면 예외
        if (fromAfter.equals(INSUFFICIENT)) {
            throw new BusinessException(ErrorCode.BIZZ_INSUFFICIENT_BALANCE);
        }

        return new RedisTransferResult(true, fromBefore, fromAfter, fromVer, toBefore, toAfter, toVer);
    }


    /**
     * Redis에 잔액이 올라와 있으면 최종 값을 꺼내고, 관련 키를 삭제
     *
     * - hit=false면 이미 만료/삭제된 상태 -> 호출자가 “DB flush 불가” 케이스로 처리
     * - hit=true면 finalBalance를 DB에 저장하고 종료하면 됨
     */
    @Timed(
            value = "buzzerbidder.redis.wallet",
            extraTags = {"op", "flush"},
            histogram = true
    )
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
            log.error("Redis flush 스크립트 반환 형식이 예상과 다릅니다. userId={}, result={}", userId, result);
            throw new BusinessException(ErrorCode.UNEXPECTED_REDIS_SCRIPT_RETURN);
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
        stringRedisTemplate.expire(SESSION_KEY_PREFIX + userId, SESSION_TTL);
        stringRedisTemplate.expire(BAL_KEY_PREFIX + userId, BALANCE_TTL);
        stringRedisTemplate.expire(VER_KEY_PREFIX + userId, BALANCE_TTL);
    }

    /**
     * "Redis 세션에 올라와 있다"를 판단하는 함수
     * - 세션키(auction:session:{userId})가 존재하고
     * - 잔액키(auction:bizz:{userId})도 존재할 때만 true
     *
     * - 세션만 있고 bal이 없으면 false로 봄
     */
    public boolean isRedisActive(Long userId) {
        if (userId == null) return false;

        String sKey = SESSION_KEY_PREFIX + userId;
        String bKey = BAL_KEY_PREFIX + userId;

        Boolean sessionExists = stringRedisTemplate.hasKey(sKey);
        Boolean balanceExists = stringRedisTemplate.hasKey(bKey);

        return Boolean.TRUE.equals(sessionExists) && Boolean.TRUE.equals(balanceExists);
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
        
            local sessionTtl = tonumber(ARGV[4])   -- SESSION_TTL.getSeconds()
            local balanceTtl = tonumber(ARGV[5])   -- BALANCE_TTL.getSeconds()
        
            local stream = ARGV[6]
            local maxlen = tonumber(ARGV[7])
            local traceId = ARGV[8]

            -- 세션이 이미 있으면 “획득 실패”
            if redis.call('EXISTS', sKey) == 1 then
              return 0
            end

            -- 세션 획득 + 초기화
            redis.call('SET', sKey, roomId, 'EX', sessionTtl)
            redis.call('SET', bKey, bal, 'EX', balanceTtl)
            redis.call('SET', vKey, '0', 'EX', balanceTtl)

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
            local sessionTtl = tonumber(ARGV[6])
            local balanceTtl = tonumber(ARGV[7])
            local stream = ARGV[8]
            local maxlen = tonumber(ARGV[9])

            --세션이 없으면 MISS
            if redis.call('EXISTS', sesKey) == 0 then
              redis.call('DEL', balKey)
              redis.call('DEL', verKey)
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
            redis.call('EXPIRE', sesKey, sessionTtl)
            redis.call('EXPIRE', balKey, balanceTtl)
            redis.call('EXPIRE', verKey, balanceTtl)

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

    private DefaultRedisScript<List<Long>> buildTransferIfPresentScript() {
        DefaultRedisScript<List<Long>> script = new DefaultRedisScript<>();

        @SuppressWarnings({"rawtypes", "unchecked"})
        Class<List<Long>> listClass = (Class) List.class;
        script.setResultType(listClass);

        script.setScriptText("""
        local fromBalKey = KEYS[1]
        local fromVerKey = KEYS[2]
        local fromSesKey = KEYS[3]
        local toBalKey   = KEYS[4]
        local toVerKey   = KEYS[5]
        local toSesKey   = KEYS[6]

        local fromUserId = ARGV[1]
        local toUserId   = ARGV[2]
        local amount     = tonumber(ARGV[3])
        local reason     = ARGV[4]
        local traceId    = ARGV[5]
        local sessionTtl = tonumber(ARGV[6])
        local balanceTtl = tonumber(ARGV[7])
        local stream     = ARGV[8]
        local maxlen     = tonumber(ARGV[9])

        -- 1) 세션 검증: 둘 중 하나라도 세션이 없으면 MISS (그리고 해당 유저 키 정리)
        if redis.call('EXISTS', fromSesKey) == 0 then
          redis.call('DEL', fromBalKey)
          redis.call('DEL', fromVerKey)
          return { %d, %d, 0, %d, %d, 0 }
        end
        if redis.call('EXISTS', toSesKey) == 0 then
          redis.call('DEL', toBalKey)
          redis.call('DEL', toVerKey)
          return { %d, %d, 0, %d, %d, 0 }
        end

        -- 2) 잔액 존재 검증: 둘 중 하나라도 bal이 없으면 MISS
        local fromCur = redis.call('GET', fromBalKey)
        local toCur = redis.call('GET', toBalKey)
        if (not fromCur) or (not toCur) then
          return { %d, %d, 0, %d, %d, 0 }
        end
        fromCur = tonumber(fromCur)
        toCur = tonumber(toCur)

        -- 3) 잔액 부족 검증: 부족하면 아무 것도 변경하지 않고 표시값 반환
        if fromCur < amount then
          local fv = tonumber(redis.call('GET', fromVerKey) or "0")
          local tv = tonumber(redis.call('GET', toVerKey) or "0")
          return { fromCur, %d, fv, toCur, toCur, tv }
        end

        -- 4) 송금 반영(원자): from 감소 + to 증가
        local fromNew = fromCur - amount
        local toNew = toCur + amount

        redis.call('SET', fromBalKey, fromNew)
        redis.call('SET', toBalKey, toNew)

        -- 5) 버전 증가(각 유저별)
        local fromVer = redis.call('INCR', fromVerKey)
        local toVer = redis.call('INCR', toVerKey)

        -- 6) TTL 연장(세션/잔액/버전 모두)
        redis.call('EXPIRE', fromSesKey, sessionTtl)
        redis.call('EXPIRE', toSesKey, sessionTtl)
        redis.call('EXPIRE', fromBalKey, balanceTtl)
        redis.call('EXPIRE', toBalKey, balanceTtl)
        redis.call('EXPIRE', fromVerKey, balanceTtl)
        redis.call('EXPIRE', toVerKey, balanceTtl)

        -- 7) 이벤트 기록(TRANSFER)
        redis.call('XADD', stream, 'MAXLEN', '~', maxlen, '*',
          'event', 'TRANSFER',
          'fromUserId', fromUserId,
          'toUserId', toUserId,
          'amount', tostring(amount),
          'fromBefore', tostring(fromCur),
          'fromAfter', tostring(fromNew),
          'toBefore', tostring(toCur),
          'toAfter', tostring(toNew),
          'fromVersion', tostring(fromVer),
          'toVersion', tostring(toVer),
          'reason', reason,
          'traceId', traceId
        )

        return { fromCur, fromNew, fromVer, toCur, toNew, toVer }
    """.formatted(
                MISS, MISS, MISS, MISS,          // from 세션 없음
                MISS, MISS, MISS, MISS,          // to 세션 없음
                MISS, MISS, MISS, MISS,          // 잔액 키 없음
                INSUFFICIENT                     // 잔액 부족 표시
        ));

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
