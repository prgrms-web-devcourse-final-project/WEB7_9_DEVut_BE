package devut.buzzerbidder.global.init;

import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {

    @Autowired
    @Lazy
    private BaseInitData self;

    private final UserRepository userRepository;
    private final LiveDealRepository liveDealRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner initDataRunner() {
        return args -> {
            self.userInitData();
            self.liveDealInitData();
        };
    }

    @Transactional
    public void userInitData() {
        if(userRepository.count() > 0) {
            return;
        }
        String email = "new@user.com";
        String encodedPassword = passwordEncoder.encode("asdf1234!");
        String nickname = "gildong";

        // 회원 생성
        User user = devut.buzzerbidder.domain.user.entity.User.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .birthDate(LocalDate.of(1990, 1, 1))
                .profileImageUrl(null)
                .role(devut.buzzerbidder.domain.user.entity.User.UserRole.USER)
                .providerType(devut.buzzerbidder.domain.user.entity.User.ProviderType.EMAIL)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public void liveDealInitData() {
        if(liveDealRepository.count() > 0) {
            return;
        }
        User buyer = userRepository.findById(1L).orElseThrow();

        LiveDeal liveDeal = LiveDeal.builder().
                item(1L). // TODO: item 추가 후 item 객체로 변경
                buyer(buyer).
                winningPrice(10000L).
                status(DealStatus.PENDING).
                trackingNumber(null).
                carrier(null).
                build();
        liveDealRepository.save(liveDeal);
    }

}