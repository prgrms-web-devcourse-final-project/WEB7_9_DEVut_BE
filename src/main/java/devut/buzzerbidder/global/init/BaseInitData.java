package devut.buzzerbidder.global.init;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.repository.AuctionRoomRepository;
import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItemImage;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.payment.entity.Payment;
import devut.buzzerbidder.domain.payment.entity.PaymentMethod;
import devut.buzzerbidder.domain.payment.repository.PaymentRepository;
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
}