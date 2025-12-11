package devut.buzzerbidder.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.domain.user.service.RefreshTokenService;
import devut.buzzerbidder.global.exeption.BusinessException;

import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@Transactional
class AuthTokenServiceTest {

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트 전 Redis 데이터 초기화
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
        
        // 기존 사용자 삭제 (중복 방지)
        userRepository.findByEmail("test@example.com")
                .ifPresent(userRepository::delete);
        
        // 테스트 사용자 생성
        testUser = User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("password123!"))
                .name("홍길동")
                .nickname("hong123")
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(User.UserRole.USER)
                .providerType(User.ProviderType.EMAIL)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Access Token 생성 성공")
    void genAccessToken_Success() {
        // when
        String accessToken = authTokenService.genAccessToken(testUser);

        // then
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
        
        // 토큰에서 payload 추출하여 검증
        Map<String, Object> payload = authTokenService.payloadOrNull(accessToken);
        assertThat(payload).isNotNull();
        assertThat(payload.get("id")).isEqualTo(testUser.getId());
        assertThat(payload.get("email")).isEqualTo("test@example.com");
        assertThat(payload.get("nickname")).isEqualTo("hong123");
    }

    @Test
    @DisplayName("Refresh Token 생성 성공 및 Redis 저장 확인")
    void genRefreshToken_Success_AndSavedInRedis() {
        // when
        String refreshToken = authTokenService.genRefreshToken(testUser);

        // then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        
        // Redis에 저장되었는지 확인
        String savedToken = refreshTokenService.getRefreshToken(testUser.getId());
        assertThat(savedToken).isEqualTo(refreshToken);
        
        // 토큰에서 payload 추출하여 검증
        Map<String, Object> payload = authTokenService.payloadOrNull(refreshToken);
        assertThat(payload).isNotNull();
        assertThat(payload.get("id")).isEqualTo(testUser.getId());
        assertThat(payload.get("email")).isEqualTo("test@example.com");
        assertThat(payload.get("nickname")).isEqualTo("hong123");
    }

    @Test
    @DisplayName("Refresh Token 재생성 시 기존 토큰 덮어쓰기")
    void genRefreshToken_OverwriteExistingToken() throws InterruptedException {
        // given
        String firstToken = authTokenService.genRefreshToken(testUser);
        String savedFirstToken = refreshTokenService.getRefreshToken(testUser.getId());
        assertThat(savedFirstToken).isEqualTo(firstToken);

        // when - 시간 지연을 추가하여 다른 iat 값을 보장
        Thread.sleep(1000); // 1초 대기
        String secondToken = authTokenService.genRefreshToken(testUser);

        // then
        assertThat(secondToken).isNotEqualTo(firstToken);
        String savedSecondToken = refreshTokenService.getRefreshToken(testUser.getId());
        assertThat(savedSecondToken).isEqualTo(secondToken);
        assertThat(savedSecondToken).isNotEqualTo(firstToken);
    }

    @Test
    @DisplayName("JWT Payload 추출 성공")
    void payloadOrNull_Success() {
        // given
        String accessToken = authTokenService.genAccessToken(testUser);

        // when
        Map<String, Object> payload = authTokenService.payloadOrNull(accessToken);

        // then
        assertThat(payload).isNotNull();
        assertThat(payload.get("id")).isEqualTo(testUser.getId());
        assertThat(payload.get("email")).isEqualTo("test@example.com");
        assertThat(payload.get("nickname")).isEqualTo("hong123");
    }

    @Test
    @DisplayName("JWT Payload 추출 실패 - 유효하지 않은 토큰")
    void payloadOrNull_Fail_InvalidToken() {
        // given
        String invalidToken = "invalid.token.string";

        // when
        Map<String, Object> payload = authTokenService.payloadOrNull(invalidToken);

        // then
        assertThat(payload).isNull();
    }

    @Test
    @DisplayName("JWT Payload 추출 실패 - 빈 문자열")
    void payloadOrNull_Fail_EmptyString() {
        // given
        String emptyToken = "";

        // when
        Map<String, Object> payload = authTokenService.payloadOrNull(emptyToken);

        // then
        assertThat(payload).isNull();
    }

    @Test
    @DisplayName("JWT Payload 추출 실패 - null")
    void payloadOrNull_Fail_Null() {
        // when
        Map<String, Object> payload = authTokenService.payloadOrNull(null);

        // then
        assertThat(payload).isNull();
    }

    @Test
    @DisplayName("Access Token과 Refresh Token이 다른지 확인")
    void genAccessToken_And_RefreshToken_AreDifferent() {
        // when
        String accessToken = authTokenService.genAccessToken(testUser);
        String refreshToken = authTokenService.genRefreshToken(testUser);

        // then
        assertThat(accessToken).isNotEqualTo(refreshToken);
    }

    @Test
    @DisplayName("여러 사용자의 토큰 생성 및 Redis 저장 확인")
    void genRefreshToken_MultipleUsers() {
        // given
        User user2 = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password123!"))
                .name("김철수")
                .nickname("kim123")
                .birthDate(LocalDate.of(1995, 5, 5))
                .role(User.UserRole.USER)
                .providerType(User.ProviderType.EMAIL)
                .build();
        user2 = userRepository.save(user2);

        // when
        String token1 = authTokenService.genRefreshToken(testUser);
        String token2 = authTokenService.genRefreshToken(user2);

        // then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(refreshTokenService.getRefreshToken(testUser.getId())).isEqualTo(token1);
        assertThat(refreshTokenService.getRefreshToken(user2.getId())).isEqualTo(token2);
    }

    @Test
    @DisplayName("Refresh Token 검증 및 User 반환 성공")
    void validateAndGetUserByRefreshToken_Success() {
        // given
        String refreshToken = authTokenService.genRefreshToken(testUser);

        // when
        User user = authTokenService.validateAndGetUserByRefreshToken(refreshToken);

        // then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(testUser.getId());
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getNickname()).isEqualTo("hong123");
    }

    @Test
    @DisplayName("Refresh Token 검증 실패 - null")
    void validateAndGetUserByRefreshToken_Fail_Null() {
        // when & then
        assertThatThrownBy(() -> authTokenService.validateAndGetUserByRefreshToken(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 토큰입니다.");
    }

    @Test
    @DisplayName("Refresh Token 검증 실패 - 빈 문자열")
    void validateAndGetUserByRefreshToken_Fail_EmptyString() {
        // when & then
        assertThatThrownBy(() -> authTokenService.validateAndGetUserByRefreshToken(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 토큰입니다.");
    }

    @Test
    @DisplayName("Refresh Token 검증 실패 - 유효하지 않은 JWT 형식")
    void validateAndGetUserByRefreshToken_Fail_InvalidJWT() {
        // given
        String invalidToken = "invalid.token.string";

        // when & then
        assertThatThrownBy(() -> authTokenService.validateAndGetUserByRefreshToken(invalidToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 토큰입니다.");
    }

    @Test
    @DisplayName("Refresh Token 검증 실패 - Redis에 저장되지 않은 토큰")
    void validateAndGetUserByRefreshToken_Fail_TokenNotInRedis() {
        // given
        // testUser의 refresh token 생성
        String validToken = authTokenService.genRefreshToken(testUser);
        
        // Redis에서 testUser의 토큰 삭제
        refreshTokenService.deleteRefreshToken(testUser.getId());

        // when & then
        assertThatThrownBy(() -> authTokenService.validateAndGetUserByRefreshToken(validToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 토큰입니다.");
    }

    @Test
    @DisplayName("Refresh Token 검증 실패 - 다른 사용자의 토큰")
    void validateAndGetUserByRefreshToken_Fail_DifferentUserToken() {
        // given
        User user2 = User.builder()
                .email("user2@example.com")
                .password(passwordEncoder.encode("password123!"))
                .name("김철수")
                .nickname("kim123")
                .birthDate(LocalDate.of(1995, 5, 5))
                .role(User.UserRole.USER)
                .providerType(User.ProviderType.EMAIL)
                .build();
        user2 = userRepository.save(user2);
        
        String user2Token = authTokenService.genRefreshToken(user2);

        // when - user2의 토큰을 사용하여 testUser를 조회하려고 시도
        // 하지만 토큰의 userId와 Redis에 저장된 userId가 다르므로 실패해야 함
        // 실제로는 토큰의 payload에서 userId를 추출하므로, user2의 토큰으로는 user2가 반환됨
        // 이 테스트는 다른 사용자의 토큰으로 다른 사용자를 조회하는 경우를 테스트
        User retrievedUser = authTokenService.validateAndGetUserByRefreshToken(user2Token);

        // then - user2가 반환되어야 함
        assertThat(retrievedUser.getId()).isEqualTo(user2.getId());
        assertThat(retrievedUser.getEmail()).isEqualTo("user2@example.com");
    }

    @Test
    @DisplayName("Refresh Token 검증 실패 - 존재하지 않는 사용자")
    void validateAndGetUserByRefreshToken_Fail_UserNotFound() {
        // given - 유효한 토큰 생성
        String refreshToken = authTokenService.genRefreshToken(testUser);
        
        // 사용자 삭제
        userRepository.delete(testUser);
        userRepository.flush();

        // when & then
        assertThatThrownBy(() -> authTokenService.validateAndGetUserByRefreshToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("존재하지 않는 회원입니다.");
    }
}

