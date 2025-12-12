package devut.buzzerbidder.global.init;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.repository.AuctionRoomRepository;
import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItemImage;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

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
    private final AuctionRoomRepository auctionRoomRepository;

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

        List<LiveItemImage> images = new ArrayList<>();
        LiveItem liveItem = LiveItem.builder()
                .sellerUserId(1L)
                .name("Sample Live Item")
                .category(LiveItem.Category.ELECTRONICS)
                .description("샘플 설명입니다.")
                .initPrice(1000000L)
                .deliveryInclude(false)
                .itemStatus(LiveItem.ItemStatus.NEW)
                .auctionStatus(LiveItem.AuctionStatus.BEFORE_BIDDING)
                .liveTime(LocalDateTime.of(2025, 12, 31, 19, 0, 0))
                .directDealAvailable(true)
                .region("서울시 강남구 역삼동")
                .preferredPlace("역삼역 근처 카페")
                .images(images)
                .build();

        liveItem.addImage(new LiveItemImage("example.com",liveItem));

        AuctionRoom newRoom = new AuctionRoom(liveItem.getLiveTime());
        auctionRoomRepository.save(newRoom);

        liveItemRepository.save(liveItem);

        newRoom.addItem(liveItem);
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