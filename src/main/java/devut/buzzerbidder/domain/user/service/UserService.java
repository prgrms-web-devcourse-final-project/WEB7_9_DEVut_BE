package devut.buzzerbidder.domain.user.service;

import devut.buzzerbidder.domain.user.dto.request.EmailLoginRequest;
import devut.buzzerbidder.domain.user.dto.request.EmailSignUpRequest;
import devut.buzzerbidder.domain.user.dto.request.UserUpdateRequest;
import devut.buzzerbidder.domain.user.dto.response.LoginResponse;
import devut.buzzerbidder.domain.user.dto.response.UserProfileResponse;
import devut.buzzerbidder.domain.user.dto.response.UserUpdateResponse;
import devut.buzzerbidder.domain.user.entity.Provider;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.ProviderRepository;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;

    @Transactional
    public LoginResponse signUp(EmailSignUpRequest request) {
        // 이메일로 기존 사용자 조회
        Optional<User> existingUser = userRepository.findByEmail(request.email());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // EMAIL Provider가 이미 존재하는 경우 (이미 이메일로 가입한 계정)
            if (providerRepository.existsByUserAndProviderType(user, Provider.ProviderType.EMAIL)) {
                throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATE);
            }
            
            // 소셜 로그인으로 가입한 계정인 경우 EMAIL Provider 추가 및 비밀번호 설정
            // 비밀번호 암호화
            String encodedPassword = passwordEncoder.encode(request.password());
            user.updatePassword(encodedPassword);
            
            // EMAIL Provider 생성
            Provider emailProvider = Provider.builder()
                    .providerType(Provider.ProviderType.EMAIL)
                    .providerId(request.email()) // EMAIL의 경우 email을 providerId로 사용
                    .user(user)
                    .build();
            providerRepository.save(emailProvider);
            
            return LoginResponse.of(user);
        }

        // 신규 사용자 생성
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
                .nickname(request.nickname())
                .birthDate(request.birthDate())
                .profileImageUrl(request.image())
                .role(User.UserRole.USER)
                .build();

        user = userRepository.save(user);

        // Provider 생성 (EMAIL)
        Provider provider = Provider.builder()
                .providerType(Provider.ProviderType.EMAIL)
                .providerId(request.email()) // EMAIL의 경우 email을 providerId로 사용
                .user(user)
                .build();
        providerRepository.save(provider);

        return LoginResponse.of(user);
    }

    public LoginResponse login(EmailLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_LOGIN_FAILED));

        // EMAIL Provider가 없는 경우 (소셜 로그인으로만 가입한 계정)
        if (!providerRepository.existsByUserAndProviderType(user, Provider.ProviderType.EMAIL)) {
            throw new BusinessException(ErrorCode.USER_SOCIAL_ACCOUNT);
        }

        // 비밀번호가 null인 경우 (이론적으로는 발생하지 않아야 하지만 안전장치)
        if (user.getPassword() == null) {
            throw new BusinessException(ErrorCode.USER_SOCIAL_ACCOUNT);
        }

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
    public UserProfileResponse getMyProfile(User user) {
        return UserProfileResponse.from(user, walletService.getBizzBalance(user));
    }

    @Transactional
    public UserUpdateResponse updateMyProfile(User user, UserUpdateRequest request) {
        // 이메일 변경 시 중복 체크
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATE);
            }
        }

        // 닉네임 변경 시 중복 체크 (null이 아니고 기존 닉네임과 다를 때만)
        if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.nickname())) {
                throw new BusinessException(ErrorCode.USER_NICKNAME_DUPLICATE);
            }
        }

        // 프로필 업데이트
        user.updateProfile(
                request.email(),
                request.nickname(),
                request.birthDate(),
                request.image()
        );

        return UserUpdateResponse.from(user);
    }
}
