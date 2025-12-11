package devut.buzzerbidder.domain.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.user.dto.request.EmailLoginRequest;
import devut.buzzerbidder.domain.user.dto.request.EmailSignUpRequest;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.domain.user.service.RefreshTokenService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@Transactional
class UserControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private EmailSignUpRequest signUpRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 각 테스트 전에 데이터 정리
        userRepository.deleteAll();
        userRepository.flush();
        
        signUpRequest = new EmailSignUpRequest(
                "test@example.com",
                "password123!",
                "홍길동",
                "hong123",
                LocalDate.of(1990, 1, 1),
                "https://example.com/image.jpg"
        );
    }

    @Test
    @DisplayName("회원가입 API 성공")
    void signUp_Success() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(signUpRequest);

        // when & then
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원가입에 성공했습니다."))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        
        // 회원가입이 실제로 성공했는지 DB에서 확인
        assert userRepository.existsByEmail("test@example.com");
    }

    @Test
    @DisplayName("회원가입 API 실패 - 이메일 중복")
    void signUp_Fail_DuplicateEmail() throws Exception {
        // given
        userRepository.save(createUser("test@example.com", "hong123"));
        String requestBody = objectMapper.writeValueAsString(signUpRequest);

        // when & then
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("M002"))
                .andExpect(jsonPath("$.msg").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("회원가입 API 실패 - 닉네임 중복")
    void signUp_Fail_DuplicateNickname() throws Exception {
        // given
        userRepository.save(createUser("another@example.com", "hong123"));
        String requestBody = objectMapper.writeValueAsString(signUpRequest);

        // when & then
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("M003"))
                .andExpect(jsonPath("$.msg").value("이미 사용 중인 닉네임입니다."));
    }

    @Test
    @DisplayName("회원가입 API 실패 - 유효성 검증 실패 (이메일 형식 오류)")
    void signUp_Fail_InvalidEmail() throws Exception {
        // given
        EmailSignUpRequest invalidRequest = new EmailSignUpRequest(
                "invalid-email",
                "password123!",
                "홍길동",
                "hong123",
                LocalDate.of(1990, 1, 1),
                null
        );
        String requestBody = objectMapper.writeValueAsString(invalidRequest);

        // when & then
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 API 성공")
    void login_Success() throws Exception {
        // given
        userRepository.save(createUser("test@example.com", "hong123"));
        EmailLoginRequest loginRequest = new EmailLoginRequest(
                "test@example.com",
                "password123!"
        );
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        // when & then
        mockMvc.perform(post("/api/v1/users/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.userInfo.email").value("test@example.com"))
                .andExpect(header().exists("Authorization"))
                .andExpect(header().exists("Refresh-Token"))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    @DisplayName("로그인 API 실패 - 존재하지 않는 이메일")
    void login_Fail_EmailNotFound() throws Exception {
        // given
        EmailLoginRequest loginRequest = new EmailLoginRequest(
                "notfound@example.com",
                "password123!"
        );
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        // when & then
        mockMvc.perform(post("/api/v1/users/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("M005"))
                .andExpect(jsonPath("$.msg").value("로그인에 실패했습니다."));
    }

    @Test
    @DisplayName("로그인 API 실패 - 잘못된 비밀번호")
    void login_Fail_WrongPassword() throws Exception {
        // given
        userRepository.save(createUser("test@example.com", "hong123"));
        EmailLoginRequest loginRequest = new EmailLoginRequest(
                "test@example.com",
                "wrongpassword123!"
        );
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        // when & then
        mockMvc.perform(post("/api/v1/users/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("M005"))
                .andExpect(jsonPath("$.msg").value("로그인에 실패했습니다."));
    }

    @Test
    @DisplayName("Access Token 재발급 API 성공")
    void refresh_Success() throws Exception {
        // given
        User user = userRepository.save(createUser("test@example.com", "hong123"));
        String refreshToken = authTokenService.genRefreshToken(user);

        // when & then
        mockMvc.perform(post("/api/v1/users/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("AccessToken 재발급에 성공했습니다."))
                .andExpect(header().exists("Authorization"))
                .andExpect(header().exists("Refresh-Token"))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    @DisplayName("Access Token 재발급 API 실패 - 쿠키에 refresh token이 없음")
    void refresh_Fail_NoRefreshToken() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/users/refresh"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("M006"))
                .andExpect(jsonPath("$.msg").value("유효하지 않은 토큰입니다."));
    }

    @Test
    @DisplayName("Access Token 재발급 API 실패 - 유효하지 않은 refresh token")
    void refresh_Fail_InvalidRefreshToken() throws Exception {
        // given
        String invalidToken = "invalid.token.string";

        // when & then
        mockMvc.perform(post("/api/v1/users/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", invalidToken)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("M006"))
                .andExpect(jsonPath("$.msg").value("유효하지 않은 토큰입니다."));
    }

    @Test
    @DisplayName("Access Token 재발급 API 실패 - Redis에 저장되지 않은 토큰")
    void refresh_Fail_TokenNotInRedis() throws Exception {
        // given
        User user = userRepository.save(createUser("test@example.com", "hong123"));
        String refreshToken = authTokenService.genRefreshToken(user);
        
        // Redis에서 토큰 삭제
        refreshTokenService.deleteRefreshToken(user.getId());

        // when & then
        mockMvc.perform(post("/api/v1/users/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("M006"))
                .andExpect(jsonPath("$.msg").value("유효하지 않은 토큰입니다."));
    }

    private User createUser(String email, String nickname) {
        return User.builder()
                .email(email)
                .password(passwordEncoder.encode("password123!"))
                .name("홍길동")
                .nickname(nickname)
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(User.UserRole.USER)
                .providerType(User.ProviderType.EMAIL)
                .build();
    }
}

