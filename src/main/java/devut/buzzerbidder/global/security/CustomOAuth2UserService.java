package devut.buzzerbidder.global.security;

import devut.buzzerbidder.domain.user.entity.Provider;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.ProviderRepository;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final WalletService walletService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 사용자 정보 로드 시작: provider={}", registrationId);
        
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        log.debug("OAuth2 사용자 속성: {}", attributes);
        
        User user = processOAuth2User(registrationId, attributes);
        
        log.info("OAuth2 사용자 처리 완료: userId={}, email={}", user.getId(), user.getEmail());
        
        // OAuth2User에 User 정보를 추가하여 SuccessHandler에서 사용할 수 있도록 함
        return new CustomOAuth2User(oAuth2User, user);
    }

    private User processOAuth2User(String registrationId, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return processKakaoUser(attributes);
        }
        if ("naver".equals(registrationId)) {
            return processNaverUser(attributes);
        }
        if ("google".equals(registrationId)) {
            return processGoogleUser(attributes);
        }
        
        throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
    }

    private User processKakaoUser(Map<String, Object> attributes) {
        // 카카오 응답 구조: {id: 123456789, kakao_account: {email: "...", profile: {...}}}
        Long kakaoId = ((Number) attributes.get("id")).longValue();
        String providerId = String.valueOf(kakaoId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        
        String email = null;
        String nickname = null;
        String profileImageUrl = null;
        
        if (kakaoAccount != null) {
            email = (String) kakaoAccount.get("email");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) {
                nickname = (String) profile.get("nickname");
                profileImageUrl = (String) profile.get("profile_image_url");
            }
        }
        
        return processOAuth2User(
                Provider.ProviderType.KAKAO,
                providerId,
                email,
                nickname,
                profileImageUrl
        );
    }

    private User processNaverUser(Map<String, Object> attributes) {
        // 네이버 응답 구조: {resultcode: "00", message: "success", response: {id: "...", email: "...", nickname: "...", profile_image: "..."}}
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        
        if (response == null) {
            throw new OAuth2AuthenticationException("네이버 사용자 정보를 가져올 수 없습니다.");
        }
        
        String providerId = (String) response.get("id");
        String email = (String) response.get("email");
        String nickname = (String) response.get("nickname");
        String profileImageUrl = (String) response.get("profile_image");
        
        return processOAuth2User(
                Provider.ProviderType.NAVER,
                providerId,
                email,
                nickname,
                profileImageUrl
        );
    }

    private User processGoogleUser(Map<String, Object> attributes) {
        // 구글 응답 구조: {sub: "123456789", email: "...", name: "...", picture: "..."}
        String providerId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String nickname = (String) attributes.get("name");
        String profileImageUrl = (String) attributes.get("picture");
        
        return processOAuth2User(
                Provider.ProviderType.GOOGLE,
                providerId,
                email,
                nickname,
                profileImageUrl
        );
    }

    /**
     * OAuth2 사용자 처리 공통 로직
     * 
     * @param providerType Provider 타입
     * @param providerId Provider ID
     * @param email 이메일
     * @param nickname 닉네임
     * @param profileImageUrl 프로필 이미지 URL
     * @return 처리된 User 엔티티
     */
    private User processOAuth2User(
            Provider.ProviderType providerType,
            String providerId,
            String email,
            String nickname,
            String profileImageUrl
    ) {
        String providerName = providerType.getProviderName();
        String defaultNicknamePrefix = providerType.getDefaultNicknamePrefix();
        // 기존 Provider 조회
        Provider provider = providerRepository.findByProviderTypeAndProviderId(
                providerType, providerId
        ).orElse(null);
        
        if (provider != null) {
            // 기존 사용자 반환 (로그인)
            User user = provider.getUser();
            // Lazy 프록시 초기화를 위해 필드 접근
            user.getEmail();
            log.info("기존 사용자 로그인: userId={}", user.getId());
            return user;
        }
        
        // Provider가 없는 경우: 이메일로 기존 사용자 조회
        if (email != null && !email.isBlank()) {
            java.util.Optional<User> existingUser = userRepository.findByEmail(email);
            
            if (existingUser.isPresent()) {
                // 기존 사용자에 Provider 추가 (계정 연결)
                User user = existingUser.get();
                log.info("기존 사용자에 소셜 로그인 Provider 추가: userId={}, email={}, provider={}", 
                        user.getId(), email, providerType);
                
                // 기존 비밀번호 보존 (기존 비밀번호가 있으면 유지)
                String existingPassword = user.getPassword();
                
                // Provider가 이미 있는지 확인 (이론적으로는 없어야 하지만 안전장치)
                if (!providerRepository.existsByUserAndProviderType(user, providerType)) {
                    Provider newProvider = Provider.builder()
                            .providerType(providerType)
                            .providerId(providerId)
                            .user(user)
                            .build();
                    providerRepository.save(newProvider);
                    log.info("{} Provider 추가 완료: userId={}", providerType, user.getId());
                }
                
                // 지갑이 없으면 생성
                if (!walletService.hasWallet(user.getId())) {
                    walletService.createWallet(user);
                    log.info("지갑 생성 완료: userId={}", user.getId());
                }
                
                // 기존 비밀번호가 있었는데 null로 변경되었다면 복원
                if (existingPassword != null && user.getPassword() == null) {
                    user.updatePassword(existingPassword);
                    log.info("기존 비밀번호 복원: userId={}", user.getId());
                }
                
                return user;
            }
        }
        
        // 신규 사용자 생성 (회원가입)
        log.info("신규 사용자 회원가입 시작: providerId={}, provider={}", providerId, providerType);
        
        // 이메일이 없을 수 있으므로 기본값 설정
        if (email == null || email.isBlank()) {
            email = providerName + "_" + providerId + "@" + providerName + ".com";
        }
        
        // 닉네임이 없을 수 있으므로 기본값 설정
        if (nickname == null || nickname.isBlank()) {
            nickname = defaultNicknamePrefix + "_" + providerId;
        }
        
        // 닉네임 중복 체크 및 처리
        String finalNickname = nickname;
        int suffix = 1;
        while (userRepository.existsByNickname(finalNickname)) {
            finalNickname = nickname + "_" + suffix;
            suffix++;
        }
        
        User user = User.builder()
                .email(email)
                .password(null) // OAuth2 사용자는 비밀번호 없음
                .nickname(finalNickname)
                .profileImageUrl(profileImageUrl)
                .role(User.UserRole.USER)
                .build();
        
        user = userRepository.save(user);
        
        // Provider 생성
        Provider newProvider = Provider.builder()
                .providerType(providerType)
                .providerId(providerId)
                .user(user)
                .build();
        providerRepository.save(newProvider);
        
        // 지갑 생성
        walletService.createWallet(user);
        // 회원가입 시 10만 bizz 지급 (임시)
        walletService.grantBizz(user, 100000L);
        log.info("신규 사용자 지갑 생성 및 10만 bizz 지급 완료: userId={}", user.getId());
        
        return user;
    }
}

