package devut.buzzerbidder.global.init;

import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
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
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {

    @Autowired
    @Lazy
    private BaseInitData self;

    private final UserRepository userRepository;
    private final LiveDealRepository liveDealRepository;
    private final LiveItemRepository liveItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner initDataRunner() {
        return args -> {
            self.userInitData();
            self.liveItemInitData();
            self.liveDealInitData();
        };
    }

    @Transactional
    public void userInitData() {
        if (userRepository.count() > 0) {
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
    public void liveItemInitData() {
        if (liveDealRepository.count() > 0) {
            return;
        }

        LiveItem liveItem = LiveItem.builder()
                .sellerUserId(1L)
                .auctionId(1L)
                .name("Sample Live Item")
                .category(LiveItem.Category.ELECTRONICS)
                .description("샘플 설명입니다.")
                .initPrice(1000000) // TODO: Long으로 변경 필요
                .deliveryInclude(false)
                .Itemstatus(LiveItem.ItemStatus.NEW) // TODO: PascalCase로 변경 필요
                .auctionStatus(LiveItem.AuctionStatus.BEFORE_BIDDING)
                .liveDate(LocalDateTime.now().plusDays(3))
                .directDealAvailable(false)
                .region("서울시 강남구 역삼동")
                .preferredPlace("역삼역 근처 카페")
                .images(null)
                .build();

        liveItemRepository.save(liveItem);
    }

    @Transactional
    public void liveDealInitData() {
        if (liveDealRepository.count() > 0) {
            return;
        }
        User buyer = userRepository.findById(1L).orElseThrow();
        LiveItem liveItem = liveItemRepository.findById(1L).orElseThrow();

        LiveDeal liveDeal = LiveDeal.builder().
                item(liveItem).
                buyer(buyer).
                winningPrice(10000L).
                status(DealStatus.PENDING).
                trackingNumber(null).
                carrier(null).
                build();

        liveDealRepository.save(liveDeal);
    }

}