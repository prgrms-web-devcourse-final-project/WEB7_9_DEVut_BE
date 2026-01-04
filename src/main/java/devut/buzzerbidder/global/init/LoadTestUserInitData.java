package devut.buzzerbidder.global.init;

import devut.buzzerbidder.domain.user.entity.Provider;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.ProviderRepository;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// JMeter 부하 테스트를 위한 테스트 유저 초기화
@Configuration
@RequiredArgsConstructor
@Profile("dev-mysql")
public class LoadTestUserInitData {

    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;

    @Bean
    public ApplicationRunner loadTestUserInitDataRunner() {
        return args -> createLoadTestUsers();
    }

    @Transactional
    public void createLoadTestUsers() {
        // 이미 테스트 유저가 생성되어 있는지 확인 (500명 기준)
        long existingTestUserCount = userRepository.count();
        if (existingTestUserCount >= 500) {
            return;
        }

        int targetUserCount = 500;
        int usersToCreate = (int) (targetUserCount - existingTestUserCount);

        String defaultPassword = "asdf1234!";
        String encodedPassword = passwordEncoder.encode(defaultPassword);

        List<User> users = new ArrayList<>();
        List<Provider> providers = new ArrayList<>();

        long startNumber = existingTestUserCount + 1;

        for (int i = 0; i < usersToCreate; i++) {
            long userNumber = startNumber + i;
            String email = String.format("test%03d@test.com", userNumber);
            String nickname = String.format("testUser_%03d", userNumber);

            User user = User.builder()
                    .email(email)
                    .password(encodedPassword)
                    .nickname(nickname)
                    .profileImageUrl(null)
                    .role(User.UserRole.USER)
                    .build();

            users.add(user);
        }

        List<User> savedUsers = userRepository.saveAll(users);

        for (User user : savedUsers) {
            Provider provider = Provider.builder()
                    .providerType(Provider.ProviderType.EMAIL)
                    .providerId(user.getEmail())
                    .user(user)
                    .build();
            providers.add(provider);

            if (!walletService.hasWallet(user.getId())) {
                walletService.createWallet(user);
            }
        }

        providerRepository.saveAll(providers);
    }
}
