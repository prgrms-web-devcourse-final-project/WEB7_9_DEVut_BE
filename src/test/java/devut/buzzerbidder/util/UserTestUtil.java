package devut.buzzerbidder.util;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 테스트용 사용자 관련 유틸리티 클래스
 */
@Component
public class UserTestUtil {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    public UserTestUtil(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
    }

    /**
     * 회원을 생성하고 User 엔티티를 반환하는 메서드
     * 
     * @param email 이메일
     * @param password 비밀번호
     * @param nickname 닉네임
     * @param profileImageUrl 프로필 이미지 URL
     * @return 생성된 User 엔티티
     */
    public User createUser(String email, String password, String nickname, String profileImageUrl) {
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        // 회원 생성
        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .name("홍길동")
                .nickname(nickname)
                .birthDate(LocalDate.of(1990, 1, 1))
                .profileImageUrl(profileImageUrl)
                .role(User.UserRole.USER)
                .providerType(User.ProviderType.EMAIL)
                .build();

        return userRepository.save(user);
    }

    /**
     * 회원을 생성하고 JWT Access Token을 반환하는 메서드
     * 
     * @param email 이메일
     * @param password 비밀번호
     * @param nickname 닉네임
     * @param profileImageUrl 프로필 이미지 URL
     * @return 생성된 Access Token
     */
    public String createUserAndGetToken(String email, String password, String nickname, String profileImageUrl) {
        User savedUser = createUser(email, password, nickname, profileImageUrl);
        // JWT Access Token 생성
        return authTokenService.genAccessToken(savedUser);
    }

    /**
     * 이메일로 저장된 User 엔티티를 조회하는 메서드
     * 
     * @param email 이메일
     * @return User 엔티티
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}

