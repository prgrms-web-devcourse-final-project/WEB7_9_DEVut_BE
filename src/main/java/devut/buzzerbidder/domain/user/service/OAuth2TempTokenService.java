package devut.buzzerbidder.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TempTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${custom.oauth2.tempTokenExpireSeconds:300}") // 기본값 5분
    private long tempTokenExpireSeconds;

    private static final String TEMP_TOKEN_PREFIX = "oauth2_temp_token:";

    /**
     * 임시 토큰 생성 및 Redis에 저장
     * @param userId 사용자 ID
     * @return 임시 토큰
     */
    public String generateTempToken(Long userId) {
        String tempToken = UUID.randomUUID().toString();
        String key = TEMP_TOKEN_PREFIX + tempToken;
        
        redisTemplate.opsForValue().set(key, userId.toString(), tempTokenExpireSeconds, TimeUnit.SECONDS);
        
        log.info("OAuth2 임시 토큰 생성: userId={}, tempToken={}", userId, tempToken);
        return tempToken;
    }

    /**
     * 임시 토큰 검증 및 사용자 ID 반환
     * @param tempToken 임시 토큰
     * @return 사용자 ID (검증 실패 시 null)
     */
    public Long validateAndGetUserId(String tempToken) {
        if (tempToken == null || tempToken.isBlank()) {
            return null;
        }

        String key = TEMP_TOKEN_PREFIX + tempToken;
        String userIdStr = redisTemplate.opsForValue().get(key);
        
        if (userIdStr == null || userIdStr.isBlank()) {
            log.warn("OAuth2 임시 토큰 검증 실패: tempToken={}", tempToken);
            return null;
        }

        try {
            Long userId = Long.parseLong(userIdStr.trim());
            // 사용 후 즉시 삭제 (1회용 토큰)
            redisTemplate.delete(key);
            log.info("OAuth2 임시 토큰 검증 성공: userId={}, tempToken={}", userId, tempToken);
            return userId;
        } catch (NumberFormatException e) {
            log.error("OAuth2 임시 토큰에서 사용자 ID 파싱 실패: userIdStr={}", userIdStr);
            return null;
        }
    }
}

