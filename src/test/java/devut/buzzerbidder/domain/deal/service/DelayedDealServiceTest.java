package devut.buzzerbidder.domain.deal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.DelayedDealRepository;
import devut.buzzerbidder.domain.delayedbid.entity.DelayedBidLog;
import devut.buzzerbidder.domain.delayedbid.repository.DelayedBidRepository;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.AuctionStatus;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.Category;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.ItemStatus;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.entity.User.UserRole;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.wallet.entity.Wallet;
import devut.buzzerbidder.domain.wallet.repository.WalletRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@Transactional
public class DelayedDealServiceTest {

    @Autowired
    private DelayedDealService delayedDealService;

    @Autowired
    private DelayedDealRepository delayedDealRepository;

    @Autowired
    private DelayedItemRepository delayedItemRepository;

    @Autowired
    private DelayedBidRepository delayedBidRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User seller;
    private User buyer;
    private DelayedItem delayedItem;

    @BeforeEach
    void setUp() {
        seller = User.builder()
            .email("seller@example.com")
            .password(passwordEncoder.encode("password123!"))
            .nickname("seller")
            .profileImageUrl("https://example.com/seller.jpg")
            .role(UserRole.USER)
            .build();
        seller = userRepository.save(seller);

        buyer = User.builder()
            .email("buyer@example.com")
            .password(passwordEncoder.encode("password123!"))
            .nickname("buyer")
            .profileImageUrl("https://example.com/buyer.jpg")
            .role(UserRole.USER)
            .build();
        buyer = userRepository.save(buyer);

        Wallet buyerWallet = walletRepository.findByUserId(buyer.getId())
            .orElseGet(() -> {
                Wallet newWallet = Wallet.builder()
                    .user(buyer)
                    .bizz(0L)
                    .build();
                return walletRepository.save(newWallet);
            });
        buyerWallet.increaseBizz(100000L);

        walletRepository.findByUserId(seller.getId())
            .orElseGet(() -> {
                Wallet newWallet = Wallet.builder()
                    .user(seller)
                    .bizz(0L)
                    .build();
                return walletRepository.save(newWallet);
            });

        delayedItem = DelayedItem.builder()
            .sellerUserId(seller.getId())
            .name("테스트 상품")
            .category(Category.ELECTRONICS)
            .description("상품 설명")
            .startPrice(10000L)
            .currentPrice(15000L)
            .endTime(LocalDateTime.now().minusMinutes(10))
            .itemStatus(ItemStatus.USED_LIKE_NEW)
            .auctionStatus(AuctionStatus.IN_DEAL)
            .deliveryInclude(true)
            .directDealAvailable(true)
            .region("서울시")
            .preferredPlace("강남역")
            .build();
        delayedItem = delayedItemRepository.save(delayedItem);
    }

    @Test
    @DisplayName("Deal 조회 실패 - 존재하지 않음")
    void t1() {
        // when & then
        assertThatThrownBy(() -> delayedDealService.findByIdOrThrow(99999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.DEAL_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("경매에서 Deal 생성 성공")
    void t2() {
        // given
        DelayedBidLog bid = DelayedBidLog.builder()
            .delayedItem(delayedItem)
            .bidderUserId(buyer.getId())
            .bidAmount(15000L)
            .bidTime(LocalDateTime.now())
            .build();
        delayedBidRepository.save(bid);

        Long sellerBalanceBefore = walletRepository.findByUserId(seller.getId())
            .orElseThrow().getBizz();

        // when
        DelayedDeal result = delayedDealService.createDealFromAuction(delayedItem.getId());

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(DealStatus.PAID);
        assertThat(result.getWinningPrice()).isEqualTo(15000L);
        assertThat(result.getBuyer().getId()).isEqualTo(buyer.getId());

        Long sellerBalanceAfter = walletRepository.findByUserId(seller.getId())
            .orElseThrow().getBizz();
        assertThat(sellerBalanceAfter).isEqualTo(sellerBalanceBefore + 15000L);
    }

    @Test
    @DisplayName("경매에서 Deal 생성 실패 - 경매 미종료")
    void t3() {
        // given
        DelayedItem ongoingItem = DelayedItem.builder()
            .sellerUserId(seller.getId())
            .name("진행중 상품")
            .category(Category.ELECTRONICS)
            .description("상품 설명")
            .startPrice(10000L)
            .currentPrice(12000L)
            .endTime(LocalDateTime.now().plusDays(1))
            .itemStatus(ItemStatus.USED_LIKE_NEW)
            .auctionStatus(AuctionStatus.IN_PROGRESS)
            .deliveryInclude(true)
            .directDealAvailable(true)
            .region("서울시")
            .preferredPlace("강남역")
            .build();
        ongoingItem = delayedItemRepository.save(ongoingItem);

        Long itemId = ongoingItem.getId();

        // when & then
        assertThatThrownBy(() -> delayedDealService.createDealFromAuction(itemId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.AUCTION_NOT_ENDED.getMessage());
    }

    @Test
    @DisplayName("경매에서 Deal 생성 실패 - 입찰 없음")
    void t4() {
        // when & then
        assertThatThrownBy(() -> delayedDealService.createDealFromAuction(delayedItem.getId()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.NO_BID_EXISTS.getMessage());
    }
}
