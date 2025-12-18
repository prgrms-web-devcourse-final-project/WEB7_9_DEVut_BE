package devut.buzzerbidder.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.user.dto.request.EmailLoginRequest;
import devut.buzzerbidder.domain.user.dto.request.EmailSignUpRequest;
import devut.buzzerbidder.domain.user.dto.response.LoginResponse;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailVerificationService emailVerificationService;

    private EmailSignUpRequest signUpRequest;

    @BeforeEach
    void setUp() {
        signUpRequest = new EmailSignUpRequest(
                "test@example.com",
                "password123!",
                "hong123",
                "https://example.com/image.jpg"
        );
    }

    @Test
    @DisplayName("회원가입 성공")
    void signUp_Success() {
        // given
        verifyEmailForSignUp("test@example.com");
        
        // when
        LoginResponse response = userService.signUp(signUpRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userInfo().email()).isEqualTo("test@example.com");
        assertThat(response.userInfo().nickname()).isEqualTo("hong123");
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signUp_Fail_DuplicateEmail() {
        // given
        verifyEmailForSignUp("test@example.com");
        userService.signUp(signUpRequest);
        
        // 다시 이메일 인증 완료 처리
        verifyEmailForSignUp("test@example.com");

        // when & then
        assertThatThrownBy(() -> userService.signUp(signUpRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_EMAIL_DUPLICATE);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // given
        verifyEmailForSignUp("test@example.com");
        userService.signUp(signUpRequest);
        EmailLoginRequest loginRequest = new EmailLoginRequest(
                "test@example.com",
                "password123!"
        );

        // when
        LoginResponse response = userService.login(loginRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userInfo().email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_Fail_EmailNotFound() {
        // given
        EmailLoginRequest loginRequest = new EmailLoginRequest(
                "notfound@example.com",
                "password123!"
        );

        // when & then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_LOGIN_FAILED);
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_Fail_WrongPassword() {
        // given
        verifyEmailForSignUp("test@example.com");
        userService.signUp(signUpRequest);
        EmailLoginRequest loginRequest = new EmailLoginRequest(
                "test@example.com",
                "wrongpassword123!"
        );

        // when & then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_LOGIN_FAILED);
    }

    @Test
    @DisplayName("비밀번호 암호화 확인")
    void signUp_PasswordEncrypted() {
        // given
        verifyEmailForSignUp("test@example.com");
        
        // when
        userService.signUp(signUpRequest);
        User savedUser = userRepository.findByEmail("test@example.com").orElseThrow();

        // then
        assertThat(savedUser.getPassword()).isNotEqualTo("password123!");
        assertThat(passwordEncoder.matches("password123!", savedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("회원 조회 성공")
    void findById_Success() {
        // given
        verifyEmailForSignUp("test@example.com");
        LoginResponse signUpResponse = userService.signUp(signUpRequest);
        Long userId = signUpResponse.userInfo().id();

        // when
        User user = userService.findById(userId);

        // then
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("회원 조회 실패 - 존재하지 않는 ID")
    void findById_Fail_NotFound() {
        // when & then
        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 인증 미완료")
    void signUp_Fail_EmailNotVerified() {
        // when & then
        // 이메일 인증을 완료하지 않고 회원가입 시도
        assertThatThrownBy(() -> userService.signUp(signUpRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_EMAIL_NOT_VERIFIED);
    }

    /**
     * 테스트용 이메일 인증 완료 처리
     * 회원가입 테스트에서 이메일 인증이 완료된 상태로 만들어줍니다.
     */
    private void verifyEmailForSignUp(String email) {
        // 인증 코드 생성 및 검증하여 인증 완료 상태로 만듦
        String code = emailVerificationService.generateAndSaveVerificationCode(email);
        emailVerificationService.verifyCode(email, code);
    }
}

