package devut.buzzerbidder.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.user.dto.request.EmailLoginRequest;
import devut.buzzerbidder.domain.user.dto.request.EmailSignUpRequest;
import devut.buzzerbidder.domain.user.dto.response.LoginResponse;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.time.LocalDate;
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

    private EmailSignUpRequest signUpRequest;

    @BeforeEach
    void setUp() {
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
    @DisplayName("회원가입 성공")
    void signUp_Success() {
        // when
        LoginResponse response = userService.signUp(signUpRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.userInfo().email()).isEqualTo("test@example.com");
        assertThat(response.userInfo().name()).isEqualTo("홍길동");
        assertThat(response.userInfo().nickname()).isEqualTo("hong123");
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signUp_Fail_DuplicateEmail() {
        // given
        userService.signUp(signUpRequest);

        // when & then
        assertThatThrownBy(() -> userService.signUp(signUpRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_EMAIL_DUPLICATE);
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void signUp_Fail_DuplicateNickname() {
        // given
        userService.signUp(signUpRequest);
        EmailSignUpRequest anotherRequest = new EmailSignUpRequest(
                "another@example.com",
                "password123!",
                "김철수",
                "hong123", // 동일한 닉네임
                LocalDate.of(1995, 5, 5),
                null
        );

        // when & then
        assertThatThrownBy(() -> userService.signUp(anotherRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NICKNAME_DUPLICATE);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // given
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
                .isEqualTo(ErrorCode.MEMBER_LOGIN_FAILED);
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_Fail_WrongPassword() {
        // given
        userService.signUp(signUpRequest);
        EmailLoginRequest loginRequest = new EmailLoginRequest(
                "test@example.com",
                "wrongpassword123!"
        );

        // when & then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_LOGIN_FAILED);
    }

    @Test
    @DisplayName("비밀번호 암호화 확인")
    void signUp_PasswordEncrypted() {
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
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}

