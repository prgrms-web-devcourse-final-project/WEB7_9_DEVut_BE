package devut.buzzerbidder.domain.user.service;

import devut.buzzerbidder.domain.user.dto.request.EmailLoginRequest;
import devut.buzzerbidder.domain.user.dto.request.EmailSignUpRequest;
import devut.buzzerbidder.domain.user.dto.response.LoginResponse;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse signUp(EmailSignUpRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATE);
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.USER_NICKNAME_DUPLICATE);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 회원 생성
        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .name(request.name())
                .nickname(request.nickname())
                .birthDate(request.birthDate())
                .profileImageUrl(request.profileImageUrl())
                .role(User.UserRole.USER)
                .providerType(User.ProviderType.EMAIL)
                .build();

        userRepository.save(user);

        return LoginResponse.of(user);
    }

    public LoginResponse login(EmailLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_LOGIN_FAILED));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_LOGIN_FAILED);
        }

        return LoginResponse.of(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
