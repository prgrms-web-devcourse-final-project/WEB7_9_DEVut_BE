package devut.buzzerbidder.domain.delayedbid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.delayedbid.dto.DelayedBidListResponse;
import devut.buzzerbidder.domain.delayedbid.dto.DelayedBidRequest;
import devut.buzzerbidder.domain.delayedbid.dto.DelayedBidResponse;
import devut.buzzerbidder.domain.delayedbid.repository.DelayedBidRepository;
import devut.buzzerbidder.domain.delayedbid.service.DelayedBidService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@Transactional
public class DelayedBidServiceTest {

    @Autowired
    private DelayedBidService delayedBidService;

    @Autowired
    private DelayedBidRepository delayedBidRepository;

    @Autowired
    private DelayedItemRepository delayedItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User seller;
    private User bidder1;
    private User bidder2;
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

        bidder1 = User.builder()
            .email("bidder1@example.com")
            .password(passwordEncoder.encode("password123!"))
            .nickname("bidder1")
            .profileImageUrl("https://example.com/bidder1.jpg")
            .role(UserRole.USER)
            .build();
        bidder1 = userRepository.save(bidder1);

        bidder2 = User.builder()
            .email("bidder2@example.com")
            .password(passwordEncoder.encode("password123!"))
            .nickname("bidder2")
            .profileImageUrl("https://example.com/bidder2.jpg")
            .role(UserRole.USER)
            .build();
        bidder2 = userRepository.save(bidder2);

        // Wallet 생성 또는 조회
        Wallet wallet1 = walletRepository.findByUserId(bidder1.getId())
            .orElseGet(() -> {
                Wallet newWallet = Wallet.builder()
                    .user(bidder1)
                    .build();
                return walletRepository.save(newWallet);
            });
        wallet1.increaseBizz(100000L);

        Wallet wallet2 = walletRepository.findByUserId(bidder2.getId())
            .orElseGet(() -> {
                Wallet newWallet = Wallet.builder()
                    .user(bidder2)
                    .build();
                return walletRepository.save(newWallet);
            });
        wallet2.increaseBizz(100000L);

        delayedItem = DelayedItem.builder()
            .sellerUserId(seller.getId())
            .name("테스트 상품")
            .category(Category.ELECTRONICS)
            .description("상품 설명")
            .startPrice(10000L)
            .currentPrice(10000L)
            .endTime(LocalDateTime.now().plusDays(5))
            .itemStatus(ItemStatus.USED_LIKE_NEW)
            .auctionStatus(AuctionStatus.BEFORE_BIDDING)
            .deliveryInclude(true)
            .directDealAvailable(true)
            .region("서울시")
            .preferredPlace("강남역")
            .build();
        delayedItem = delayedItemRepository.save(delayedItem);
    }

    @Test
    @DisplayName("첫 입찰 성공 - BEFORE_BIDDING에서 IN_PROGRESS로 전환")
    void t1() {
        // given
        DelayedBidRequest request = new DelayedBidRequest(15000L);

        // when
        DelayedBidResponse response = delayedBidService.placeBid(
            delayedItem.getId(),
            request,
            bidder1
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.bidPrice()).isEqualTo(15000L);
        assertThat(response.bidderNickname()).isEqualTo("bidder1");

        // 상태 전환 확인
        DelayedItem updated = delayedItemRepository.findById(delayedItem.getId()).orElseThrow();
        assertThat(updated.getAuctionStatus()).isEqualTo(AuctionStatus.IN_PROGRESS);
        assertThat(updated.getCurrentPrice()).isEqualTo(15000L);

        // 코인 차감 확인
        Wallet wallet = walletRepository.findByUserId(bidder1.getId()).orElseThrow();
        assertThat(wallet.getBizz()).isEqualTo(85000L);
    }

    @Test
    @DisplayName("두번째 입찰 성공 - 이전 최고가 입찰자에게 환불")
    void t2() {
        // given
        DelayedBidRequest request1 = new DelayedBidRequest(15000L);
        delayedBidService.placeBid(delayedItem.getId(), request1, bidder1);

        Wallet wallet1Before = walletRepository.findByUserId(bidder1.getId()).orElseThrow();
        Long bidder1BalanceBefore =  wallet1Before.getBizz();

        // when
        DelayedBidRequest request2 = new DelayedBidRequest(20000L);
        DelayedBidResponse response = delayedBidService.placeBid(
            delayedItem.getId(),
            request2,
            bidder2
        );

        // then
        assertThat(response.bidPrice()).isEqualTo(20000L);
        assertThat(response.bidderNickname()).isEqualTo("bidder2");

        // 이전 입찰자에게 환불 확인
        Wallet wallet1After = walletRepository.findByUserId(bidder1.getId()).orElseThrow();
        assertThat(wallet1After.getBizz()).isEqualTo(bidder1BalanceBefore + 15000L);

        // 새 입찰자 코인 차감 확인
        Wallet wallet2 = walletRepository.findByUserId(bidder2.getId()).orElseThrow();
        assertThat(wallet2.getBizz()).isEqualTo(80000L);

        // 현재가 업데이트 확인
        DelayedItem updated = delayedItemRepository.findById(delayedItem.getId()).orElseThrow();
        assertThat(updated.getCurrentPrice()).isEqualTo(20000L);
    }

    @Test
    @DisplayName("입찰 실패 - 본인 물품 입찰 불가")
    void t3() {
        // given
        DelayedBidRequest request = new DelayedBidRequest(15000L);

        // when & then
        assertThatThrownBy(() ->
            delayedBidService.placeBid(delayedItem.getId(), request, seller))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.CANNOT_BID_OWN_ITEM.getMessage());
    }

    @Test
    @DisplayName("입찰 실패 - 현재가보다 낮은 금액")
    void t4() {
        // given
        DelayedBidRequest request1 = new DelayedBidRequest(15000L);
        delayedBidService.placeBid(delayedItem.getId(), request1, bidder1);

        // when & then
        DelayedBidRequest request2 = new DelayedBidRequest(14000L);
        assertThatThrownBy(() ->
            delayedBidService.placeBid(delayedItem.getId(), request2, bidder2))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.BID_PRICE_TOO_LOW.getMessage());
    }

    @Test
    @DisplayName("입찰 실패 - 경매 종료된 물품")
    void t5() {
        // given
        DelayedItem endedItem = DelayedItem.builder()
            .sellerUserId(seller.getId())
            .name("종료된 상품")
            .category(Category.ELECTRONICS)
            .description("상품 설명")
            .startPrice(10000L)
            .currentPrice(10000L)
            .endTime(LocalDateTime.now().minusDays(1))
            .itemStatus(ItemStatus.USED_LIKE_NEW)
            .auctionStatus(AuctionStatus.IN_PROGRESS)
            .deliveryInclude(true)
            .directDealAvailable(true)
            .region("서울시")
            .preferredPlace("강남역")
            .build();
        endedItem = delayedItemRepository.save(endedItem);

        DelayedBidRequest request = new DelayedBidRequest(15000L);

        // when & then
        Long endedItemId = endedItem.getId();
        assertThatThrownBy(() ->
            delayedBidService.placeBid(endedItemId, request, bidder1))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.AUCTION_ALREADY_ENDED.getMessage());
    }

    @Test
    @DisplayName("입찰 실패 - 이미 최고가 입찰자")
    void t6() {
        // given
        DelayedBidRequest request1 = new DelayedBidRequest(15000L);
        delayedBidService.placeBid(delayedItem.getId(), request1, bidder1);

        // when & then
        DelayedBidRequest request2 = new DelayedBidRequest(20000L);
        assertThatThrownBy(() ->
            delayedBidService.placeBid(delayedItem.getId(), request2, bidder1))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.ALREADY_HIGHEST_BIDDER.getMessage());
    }

    @Test
    @DisplayName("입찰 실패 - 코인 부족")
    void t7() {
        // given
        Wallet wallet = walletRepository.findByUserId(bidder1.getId()).orElseThrow();
        wallet.decreaseBizz(95000L);

        DelayedBidRequest request = new DelayedBidRequest(15000L);

        // when & then
        assertThatThrownBy(() ->
            delayedBidService.placeBid(delayedItem.getId(), request, bidder1))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.BIZZ_INSUFFICIENT_BALANCE.getMessage());
    }

    @Test
    @DisplayName("입찰 내역 조회 성공")
    void t8() {
        // given
        DelayedBidRequest request1 = new DelayedBidRequest(15000L);
        delayedBidService.placeBid(delayedItem.getId(), request1, bidder1);

        DelayedBidRequest request2 = new DelayedBidRequest(20000L);
        delayedBidService.placeBid(delayedItem.getId(), request2, bidder2);

        DelayedBidRequest request3 = new DelayedBidRequest(25000L);
        delayedBidService.placeBid(delayedItem.getId(), request3, bidder1);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        DelayedBidListResponse response = delayedBidService.getBidHistory(
            delayedItem.getId(),
            pageable
        );

        // then
        assertThat(response.bids()).hasSize(3);
        assertThat(response.totalCount()).isEqualTo(3);

        // 최신 입찰이 먼저 (내림차순)
        assertThat(response.bids().get(0).bidPrice()).isEqualTo(25000L);
        assertThat(response.bids().get(1).bidPrice()).isEqualTo(20000L);
        assertThat(response.bids().get(2).bidPrice()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("내 입찰 내역 조회 성공")
    void t9() {
        // given
        DelayedBidRequest request1 = new DelayedBidRequest(15000L);
        delayedBidService.placeBid(delayedItem.getId(), request1, bidder1);

        DelayedBidRequest request2 = new DelayedBidRequest(20000L);
        delayedBidService.placeBid(delayedItem.getId(), request2, bidder2);

        DelayedBidRequest request3 = new DelayedBidRequest(25000L);
        delayedBidService.placeBid(delayedItem.getId(), request3, bidder1);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        DelayedBidListResponse response = delayedBidService.getMyBids(bidder1, pageable);

        // then
        assertThat(response.bids()).hasSize(2);
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.bids().get(0).bidPrice()).isEqualTo(25000L);
        assertThat(response.bids().get(1).bidPrice()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("입찰 내역 조회 실패 - 존재하지 않는 경매")
    void t10() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() ->
            delayedBidService.getBidHistory(99999L, pageable))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.NOT_FOUND_DATA.getMessage());
    }
}
