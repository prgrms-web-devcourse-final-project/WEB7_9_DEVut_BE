package devut.buzzerbidder.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.user.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // 테스트 전 Redis 데이터 초기화
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("Refresh Token 저장 성공")
    void saveRefreshToken_Success() {
        // given
        Long userId = 1L;
        String refreshToken = "test-refresh-token-12345";

        // when
        refreshTokenService.saveRefreshToken(userId, refreshToken);

        // then
        String savedToken = refreshTokenService.getRefreshToken(userId);
        assertThat(savedToken).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("Refresh Token 조회 성공")
    void getRefreshToken_Success() {
        // given
        Long userId = 1L;
        String refreshToken = "test-refresh-token-12345";
        refreshTokenService.saveRefreshToken(userId, refreshToken);

        // when
        String retrievedToken = refreshTokenService.getRefreshToken(userId);

        // then
        assertThat(retrievedToken).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("Refresh Token 조회 실패 - 존재하지 않는 토큰")
    void getRefreshToken_Fail_NotFound() {
        // given
        Long userId = 999L;

        // when
        String retrievedToken = refreshTokenService.getRefreshToken(userId);

        // then
        assertThat(retrievedToken).isNull();
    }

    @Test
    @DisplayName("Refresh Token 검증 성공")
    void validateRefreshToken_Success() {
        // given
        Long userId = 1L;
        String refreshToken = "test-refresh-token-12345";
        refreshTokenService.saveRefreshToken(userId, refreshToken);

        // when
        boolean isValid = refreshTokenService.validateRefreshToken(userId, refreshToken);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Refresh Token 검증 실패 - 다른 토큰")
    void validateRefreshToken_Fail_DifferentToken() {
        // given
        Long userId = 1L;
        String savedToken = "test-refresh-token-12345";
        String differentToken = "different-token-67890";
        refreshTokenService.saveRefreshToken(userId, savedToken);

        // when
        boolean isValid = refreshTokenService.validateRefreshToken(userId, differentToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Refresh Token 검증 실패 - 존재하지 않는 사용자")
    void validateRefreshToken_Fail_UserNotFound() {
        // given
        Long userId = 999L;
        String refreshToken = "test-refresh-token-12345";

        // when
        boolean isValid = refreshTokenService.validateRefreshToken(userId, refreshToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Refresh Token 삭제 성공")
    void deleteRefreshToken_Success() {
        // given
        Long userId = 1L;
        String refreshToken = "test-refresh-token-12345";
        refreshTokenService.saveRefreshToken(userId, refreshToken);
        assertThat(refreshTokenService.getRefreshToken(userId)).isNotNull();

        // when
        refreshTokenService.deleteRefreshToken(userId);

        // then
        String deletedToken = refreshTokenService.getRefreshToken(userId);
        assertThat(deletedToken).isNull();
    }

    @Test
    @DisplayName("Refresh Token TTL 확인 - 만료 시간 설정")
    void saveRefreshToken_WithTTL() {
        // given
        Long userId = 1L;
        String refreshToken = "test-refresh-token-12345";

        // when
        refreshTokenService.saveRefreshToken(userId, refreshToken);

        // then
        String savedToken = refreshTokenService.getRefreshToken(userId);
        assertThat(savedToken).isEqualTo(refreshToken);

        // TTL이 설정되어 있는지 확인 (Redis에서 직접 확인)
        String key = "refresh_token:" + userId;
        Long ttl = redisTemplate.getExpire(key);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(0);
    }

    @Test
    @DisplayName("여러 사용자의 Refresh Token 저장 및 조회")
    void saveRefreshToken_MultipleUsers() {
        // given
        Long userId1 = 1L;
        Long userId2 = 2L;
        String token1 = "token-user-1";
        String token2 = "token-user-2";

        // when
        refreshTokenService.saveRefreshToken(userId1, token1);
        refreshTokenService.saveRefreshToken(userId2, token2);

        // then
        assertThat(refreshTokenService.getRefreshToken(userId1)).isEqualTo(token1);
        assertThat(refreshTokenService.getRefreshToken(userId2)).isEqualTo(token2);
    }
}

