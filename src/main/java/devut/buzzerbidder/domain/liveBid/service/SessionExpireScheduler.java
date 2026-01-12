package devut.buzzerbidder.domain.liveBid.service;

import devut.buzzerbidder.domain.wallet.service.WalletRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionExpireScheduler {

    private final StringRedisTemplate redis;
    private final WalletRedisService walletRedisService;

    private static final String SESSION_PREFIX = "auction:session:";
    private static final String SESSION_EXP_ZSET = "auction:sessions:exp";

    // 한 번에 처리할 최대 개수(배치 제한)
    private static final int BATCH_SIZE = 100;

    /**
     * score <= nowMs 인 멤버를 최대 limit개 꺼내고, 같은 스크립트 안에서 제거까지 수행
     */
    private static final String POP_DUE_MEMBERS_SCRIPT = """
        local zkey = KEYS[1]
        local nowMs = tonumber(ARGV[1])
        local limit = tonumber(ARGV[2])

        if (not limit) or limit <= 0 then
          return {}
        end

        local members = redis.call('ZRANGEBYSCORE', zkey, '-inf', nowMs, 'LIMIT', 0, limit)
        if (not members) or (#members == 0) then
          return {}
        end
    
        redis.call('ZREM', zkey, unpack(members))
        return members
    """;

    private final DefaultRedisScript<List<String>> popDueMembersScript = buildPopDueMembersScript();

    @Scheduled(fixedDelay = 500)
    public void processExpiredSessions() {
        long nowMs = System.currentTimeMillis();

        // 원자적으로 due userId들을 꺼냄(다른 서버와 경합해도 중복 감소)
        List<String> userIds = executeStringList(
                popDueMembersScript,
                List.of(SESSION_EXP_ZSET),
                String.valueOf(nowMs),
                String.valueOf(BATCH_SIZE)
        );

        if (userIds.isEmpty()) return;

        for (String userIdStr : userIds) {
            String sessionKey = SESSION_PREFIX + userIdStr;

            try {
                // 1) 진짜 만료됐는지 최종 확인
                Boolean exists = redis.hasKey(sessionKey);
                if (Boolean.TRUE.equals(exists)) {
                    // 아직 살아있으면(heartbeat가 늦게 갱신) TTL로 다시 스케줄
                    Long ttlSec = redis.getExpire(sessionKey, TimeUnit.SECONDS);
                    if (ttlSec != null && ttlSec > 0) {
                        long expireAtMs = System.currentTimeMillis() + ttlSec * 1000L;
                        redis.opsForZSet().add(SESSION_EXP_ZSET, userIdStr, expireAtMs);
                    }
                    continue;
                }

                // 2) 만료 확정이면 flush
                walletRedisService.flushBalanceAndClearSession(Long.parseLong(userIdStr), null);

            } catch (Exception e) {
                // 실패 시 로그
                log.error("SessionExpireScheduler failed. userId={}", userIdStr, e);
            }
        }
    }

    /* ==================== Lua Script 빌더/실행 헬퍼 ==================== */

    private DefaultRedisScript<List<String>> buildPopDueMembersScript() {
        DefaultRedisScript<List<String>> script = new DefaultRedisScript<>();

        @SuppressWarnings({"rawtypes", "unchecked"})
        Class<List<String>> listClass = (Class) List.class;
        script.setResultType(listClass);

        script.setScriptText(POP_DUE_MEMBERS_SCRIPT);
        return script;
    }

    private List<String> executeStringList(DefaultRedisScript<List<String>> script, List<String> keys, String... args) {
        List<String> result = redis.execute(script, keys, (Object[]) args);
        return Objects.requireNonNullElse(result, List.of());
    }
}
