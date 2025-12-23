package devut.buzzerbidder.global.init;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.repository.AuctionRoomRepository;
import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItemImage;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.likedelayed.entity.LikeDelayed;
import devut.buzzerbidder.domain.likedelayed.repository.LikeDelayedRepository;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItemImage;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.payment.entity.Payment;
import devut.buzzerbidder.domain.payment.entity.PaymentMethod;
import devut.buzzerbidder.domain.payment.repository.PaymentRepository;
import devut.buzzerbidder.domain.likelive.entity.LikeLive;
import devut.buzzerbidder.domain.likelive.repository.LikeLiveRepository;
import devut.buzzerbidder.domain.user.dto.request.EmailSignUpRequest;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.domain.wallet.entity.Wallet;
import devut.buzzerbidder.domain.wallet.entity.WalletHistory;
import devut.buzzerbidder.domain.wallet.enums.WalletTransactionType;
import devut.buzzerbidder.domain.wallet.repository.WalletHistoryRepository;
import devut.buzzerbidder.domain.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {

    @Autowired
    @Lazy
    private BaseInitData self;

    private final RedisTemplate<String, String> redisTemplate;
    private final UserService userService;
    private final UserRepository userRepository;
    private final LiveDealRepository liveDealRepository;
    private final LiveItemRepository liveItemRepository;
    private final DelayedItemRepository delayedItemRepository;
    private final LikeLiveRepository likeLiveRepository;
    private final LikeDelayedRepository likeDelayedRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuctionRoomRepository auctionRoomRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final WalletHistoryRepository walletHistoryRepository;

    @Bean
    ApplicationRunner initDataRunner() {
        return args -> {
            self.userInitData();
            self.adminInitData();
            self.liveItemInitData();
            self.liveDealInitData();
            self.paymentInitData();
            self.walletInitData();
            self.myItemsInitData();
            self.myLikedItemsInitData();
        };
    }

    @Transactional
    public void userInitData() {
        if (userRepository.count() > 0) {
            return;
        }
        String email = "new@user.com";
        String password = "asdf1234!";
        String nickname = "gildong";

        EmailSignUpRequest signUpRequest = new EmailSignUpRequest(
                email,
                password,
                nickname,
                null
        );

        String verifiedKey = "verified_email:" + email;
        redisTemplate.opsForValue().set(verifiedKey, "verified", 10, TimeUnit.SECONDS);
        userService.signUp(signUpRequest);
    }

    @Transactional
    public void adminInitData() {
        String email = "admin@user.com";
        String password = "asdf1234!";
        String nickname = "admin";

        if (userRepository.existsByEmail(email)) return;

        String verifiedKey = "verified_email:" + email;
        redisTemplate.opsForValue().set(verifiedKey, "verified", 10, TimeUnit.SECONDS);

        EmailSignUpRequest request = new EmailSignUpRequest(email, password, nickname, null);
        userService.signUp(request); // LoginResponse 반환

        User admin = userRepository.findByEmail(email).orElseThrow();

        admin.changeRole(User.UserRole.ADMIN);
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

        AuctionRoom newRoom = new AuctionRoom(liveItem.getLiveTime(), 1L);
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

    @Transactional
    public void paymentInitData() {
        if (paymentRepository.count() > 0) {
            return;
        }

        User user = userRepository.findByEmail("new@user.com").orElseThrow();

        // 1) 성공 결제 1건
        Payment p1 = new Payment(user, "ORDER-001", "테스트 결제 1", 10000L);
        p1.confirm(
                "pay_test_key_001",
                PaymentMethod.EASY_PAY,
                OffsetDateTime.now().minusDays(3)
        );

        Payment p2 = new Payment(user, "ORDER-002", "테스트 결제 2", 15000L);
        p2.fail("FAIL-001", "잔액 부족");

        paymentRepository.saveAll(List.of(p1, p2));
    }

    @Transactional
    public void walletInitData() {

        User user = userRepository.findByEmail("new@user.com").orElseThrow();
        Wallet wallet = walletRepository.findById(1L).orElseThrow();

        Long chargeAmount = 10000L;
        Long before = wallet.getBizz();
        wallet.increaseBizz(chargeAmount);
        Long after = wallet.getBizz();

        WalletHistory walletHistory = WalletHistory.builder()
                .user(user)
                .amount(chargeAmount)
                .type(WalletTransactionType.CHARGE)
                .bizzBalanceBefore(before)
                .bizzBalanceAfter(after)
                .build();

        walletRepository.save(wallet);
        walletHistoryRepository.save(walletHistory);
    }

    public void myItemsInitData() {
        // 이미 데이터가 있으면 스킵
        if (liveItemRepository.count() > 1 || delayedItemRepository.count() > 0) {
            return;
        }

        User user = userRepository.findById(1L).orElseThrow();

        // 내가 작성한 LiveItem 추가
        List<LiveItemImage> images2 = new ArrayList<>();
        LiveItem myLiveItem = LiveItem.builder()
                .sellerUserId(user.getId())
                .name("내가 작성한 라이브 경매 상품")
                .category(LiveItem.Category.CLOTHES)
                .description("내가 직접 등록한 라이브 경매 상품입니다.")
                .initPrice(50000L)
                .deliveryInclude(true)
                .itemStatus(LiveItem.ItemStatus.USED_LIKE_NEW)
                .auctionStatus(LiveItem.AuctionStatus.BEFORE_BIDDING)
                .liveTime(LocalDateTime.of(2025, 12, 15, 14, 0, 0))
                .directDealAvailable(false)
                .region("서울시 강남구")
                .preferredPlace(null)
                .images(images2)
                .build();

        myLiveItem.addImage(new LiveItemImage("https://example.com/my-live-item.jpg", myLiveItem));

        AuctionRoom myRoom = new AuctionRoom(myLiveItem.getLiveTime(), 1L);
        auctionRoomRepository.save(myRoom);
        liveItemRepository.save(myLiveItem);
        myRoom.addItem(myLiveItem);

        // 내가 작성한 DelayedItem 추가
        List<DelayedItemImage> delayedImages = new ArrayList<>();
        DelayedItem myDelayedItem = DelayedItem.builder()
                .sellerUserId(user.getId())
                .name("내가 작성한 지연 경매 상품")
                .category(DelayedItem.Category.ELECTRONICS)
                .description("내가 직접 등록한 지연 경매 상품입니다.")
                .startPrice(200000L)
                .currentPrice(200000L)
                .endTime(LocalDateTime.of(2025, 12, 20, 23, 59, 59))
                .itemStatus(DelayedItem.ItemStatus.NEW)
                .auctionStatus(DelayedItem.AuctionStatus.BEFORE_BIDDING)
                .deliveryInclude(true)
                .directDealAvailable(true)
                .region("서울시 서초구")
                .preferredPlace("서초역 근처")
                .images(delayedImages)
                .build();

        myDelayedItem.addImage(new DelayedItemImage("https://example.com/my-delayed-item.jpg", myDelayedItem));
        delayedItemRepository.save(myDelayedItem);
    }

    @Transactional
    public void myLikedItemsInitData() {
        // 이미 좋아요 데이터가 있으면 스킵
        if (likeLiveRepository.count() > 0 || likeDelayedRepository.count() > 0) {
            return;
        }

        User user = userRepository.findById(1L).orElseThrow();

        // 다른 사용자가 작성한 LiveItem 생성 후 좋아요 추가
        List<LiveItemImage> likedLiveImages = new ArrayList<>();
        LiveItem likedLiveItem = LiveItem.builder()
                .sellerUserId(999L) // 다른 사용자 ID (실제로는 존재하지 않지만 테스트용)
                .name("다른 사용자가 작성한 라이브 경매 상품")
                .category(LiveItem.Category.ART)
                .description("이 상품을 좋아요 했습니다.")
                .initPrice(300000L)
                .deliveryInclude(true)
                .itemStatus(LiveItem.ItemStatus.NEW)
                .auctionStatus(LiveItem.AuctionStatus.BEFORE_BIDDING)
                .liveTime(LocalDateTime.of(2025, 12, 18, 16, 30, 0))
                .directDealAvailable(false)
                .region("서울시 종로구")
                .preferredPlace(null)
                .images(likedLiveImages)
                .build();

        likedLiveItem.addImage(new LiveItemImage("https://example.com/liked-live-item.jpg", likedLiveItem));

        AuctionRoom likedRoom = new AuctionRoom(likedLiveItem.getLiveTime(),1L);
        auctionRoomRepository.save(likedRoom);
        liveItemRepository.save(likedLiveItem);
        likedRoom.addItem(likedLiveItem);

        // 좋아요 추가
        LikeLive likeLive = new LikeLive(user, likedLiveItem);
        likeLiveRepository.save(likeLive);

        // 다른 사용자가 작성한 DelayedItem 생성 후 좋아요 추가
        List<DelayedItemImage> likedDelayedImages = new ArrayList<>();
        DelayedItem likedDelayedItem = DelayedItem.builder()
                .sellerUserId(999L) // 다른 사용자 ID (실제로는 존재하지 않지만 테스트용)
                .name("다른 사용자가 작성한 지연 경매 상품")
                .category(DelayedItem.Category.SPORTS)
                .description("이 상품을 좋아요 했습니다.")
                .startPrice(150000L)
                .currentPrice(150000L)
                .endTime(LocalDateTime.of(2025, 12, 25, 23, 59, 59))
                .itemStatus(DelayedItem.ItemStatus.USED_LIKE_NEW)
                .auctionStatus(DelayedItem.AuctionStatus.BEFORE_BIDDING)
                .deliveryInclude(false)
                .directDealAvailable(true)
                .region("서울시 마포구")
                .preferredPlace("홍대입구역")
                .images(likedDelayedImages)
                .build();

        likedDelayedItem.addImage(new DelayedItemImage("https://example.com/liked-delayed-item.jpg", likedDelayedItem));
        delayedItemRepository.save(likedDelayedItem);

        // 좋아요 추가
        LikeDelayed likeDelayed = new LikeDelayed(user, likedDelayedItem);
        likeDelayedRepository.save(likeDelayed);
    }
}