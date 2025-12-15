package devut.buzzerbidder.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import devut.buzzerbidder.domain.user.dto.request.UserUpdateRequest;
import devut.buzzerbidder.domain.user.entity.Provider;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.ProviderRepository;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.domain.user.service.RefreshTokenService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private EmailSignUpRequest signUpRequest;

    @BeforeEach
    void setUp() {
        // @Transactional 어노테이션으로 인해 각 테스트 후 자동 롤백되므로
        // 별도로 데이터를 삭제할 필요가 없습니다.
        
        signUpRequest = new EmailSignUpRequest(
                "test@example.com",
                "password123!",
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

    @Test
    @DisplayName("내 정보 조회 API 성공")
    void getMyProfile_Success() throws Exception {
        // given
        User user = userRepository.save(createUser("test@example.com", "hong123"));
        String accessToken = authTokenService.genAccessToken(user);

        // when & then
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원정보 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("hong123"))
                .andExpect(jsonPath("$.data.birth").value("1990-01-01"));
    }

    @Test
    @DisplayName("내 정보 조회 API 실패 - 인증 없음")
    void getMyProfile_Fail_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/users/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 정보 수정 API 성공")
    void updateMyProfile_Success() throws Exception {
        // given
        User user = userRepository.save(createUser("test@example.com", "hong123"));
        String accessToken = authTokenService.genAccessToken(user);

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "updated@example.com",
                "updatedNickname",
                LocalDate.of(1995, 5, 5),
                "https://example.com/updated.jpg"
        );
        String requestBody = objectMapper.writeValueAsString(updateRequest);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("회원정보 수정 성공"))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("updatedNickname"))
                .andExpect(jsonPath("$.data.birth").value("1995-05-05"))
                .andExpect(jsonPath("$.data.image").value("https://example.com/updated.jpg"));

        // DB에서 실제로 수정되었는지 확인
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assert updatedUser.getEmail().equals("updated@example.com");
        assert updatedUser.getNickname().equals("updatedNickname");
    }

    @Test
    @DisplayName("내 정보 수정 API 성공 - 부분 수정 (nickname만)")
    void updateMyProfile_Success_PartialUpdate() throws Exception {
        // given
        User user = userRepository.save(createUser("test@example.com", "hong123"));
        String accessToken = authTokenService.genAccessToken(user);

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                null, // email은 변경하지 않음
                "newNickname",
                null, // birth는 변경하지 않음
                null  // image는 변경하지 않음
        );
        String requestBody = objectMapper.writeValueAsString(updateRequest);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.email").value("test@example.com")) // 기존 값 유지
                .andExpect(jsonPath("$.data.nickname").value("newNickname")) // 변경됨
                .andExpect(jsonPath("$.data.birth").value("1990-01-01")); // 기존 값 유지
    }

    @Test
    @DisplayName("내 정보 수정 API 실패 - 인증 없음")
    void updateMyProfile_Fail_Unauthorized() throws Exception {
        // given
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "updated@example.com",
                "updatedNickname",
                LocalDate.of(1995, 5, 5),
                null
        );
        String requestBody = objectMapper.writeValueAsString(updateRequest);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 정보 수정 API 실패 - 이메일 중복")
    void updateMyProfile_Fail_DuplicateEmail() throws Exception {
        // given
        User user1 = userRepository.save(createUser("test1@example.com", "user1"));
        User user2 = userRepository.save(createUser("test2@example.com", "user2"));
        String accessToken = authTokenService.genAccessToken(user1);

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "test2@example.com", // 이미 사용 중인 이메일
                "user1",
                LocalDate.of(1990, 1, 1),
                null
        );
        String requestBody = objectMapper.writeValueAsString(updateRequest);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("M002"))
                .andExpect(jsonPath("$.msg").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("내 정보 수정 API 실패 - 닉네임 중복")
    void updateMyProfile_Fail_DuplicateNickname() throws Exception {
        // given
        User user1 = userRepository.save(createUser("test1@example.com", "user1"));
        User user2 = userRepository.save(createUser("test2@example.com", "user2"));
        String accessToken = authTokenService.genAccessToken(user1);

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "test1@example.com",
                "user2", // 이미 사용 중인 닉네임
                LocalDate.of(1990, 1, 1),
                null
        );
        String requestBody = objectMapper.writeValueAsString(updateRequest);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("M003"))
                .andExpect(jsonPath("$.msg").value("이미 사용 중인 닉네임입니다."));
    }

    @Test
    @DisplayName("내 정보 수정 API 실패 - 유효성 검증 실패 (이메일 형식 오류)")
    void updateMyProfile_Fail_InvalidEmail() throws Exception {
        // given
        User user = userRepository.save(createUser("test@example.com", "hong123"));
        String accessToken = authTokenService.genAccessToken(user);

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "invalid-email", // 잘못된 이메일 형식
                "hong123",
                LocalDate.of(1990, 1, 1),
                null
        );
        String requestBody = objectMapper.writeValueAsString(updateRequest);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("내 정보 수정 API 성공 - 자신의 이메일로 변경 (중복 아님)")
    void updateMyProfile_Success_SameEmail() throws Exception {
        // given
        User user = userRepository.save(createUser("test@example.com", "hong123"));
        String accessToken = authTokenService.genAccessToken(user);

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "test@example.com", // 자신의 기존 이메일
                "newNickname",
                LocalDate.of(1995, 5, 5),
                null
        );
        String requestBody = objectMapper.writeValueAsString(updateRequest);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("newNickname"));
    }

    @Test
    @DisplayName("내 정보 수정 API 성공 - 자신의 닉네임으로 변경 (중복 아님)")
    void updateMyProfile_Success_SameNickname() throws Exception {
        // given
        User user = userRepository.save(createUser("test@example.com", "hong123"));
        String accessToken = authTokenService.genAccessToken(user);

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "updated@example.com",
                "hong123", // 자신의 기존 닉네임
                LocalDate.of(1995, 5, 5),
                null
        );
        String requestBody = objectMapper.writeValueAsString(updateRequest);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("hong123"));
    }

    private User createUser(String email, String nickname) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("password123!"))
                .nickname(nickname)
                .birthDate(LocalDate.of(1990, 1, 1))
                .role(User.UserRole.USER)
                .build();
        
        user = userRepository.save(user);
        
        // EMAIL Provider 생성 (이메일 로그인을 위해 필요)
        Provider provider = Provider.builder()
                .providerType(Provider.ProviderType.EMAIL)
                .providerId(email)
                .user(user)
                .build();
        providerRepository.save(provider);
        
        return user;
    }
}

