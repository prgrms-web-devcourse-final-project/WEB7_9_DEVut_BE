package devut.buzzerbidder.domain.deal.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.event.DelayedAuctionEndedEvent;
import devut.buzzerbidder.domain.deal.service.DelayedDealService;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.AuctionStatus;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.Category;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.ItemStatus;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.entity.User.UserRole;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class DelayedAuctionProcessorTest {

    @InjectMocks
    private DelayedAuctionProcessor delayedAuctionProcessor;

    @Mock
    private DelayedItemRepository delayedItemRepository;

    @Mock
    private DelayedDealService delayedDealService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DelayedItem endedItemWithBids;
    private DelayedItem endedItemNoBids;
    private DelayedDeal mockDeal;
    private User mockBuyer;

    @BeforeEach
    void setUp() {
        mockBuyer = User.builder()
            .email("buyer@example.com")
            .nickname("buyer")
            .role(UserRole.USER)
            .build();
        ReflectionTestUtils.setField(mockBuyer, "id", 2L);

        endedItemWithBids = DelayedItem.builder()
            .sellerUserId(1L)
            .name("입찰 있는 경매")
            .category(Category.ELECTRONICS)
            .description("상품 설명")
            .startPrice(10000L)
            .currentPrice(15000L)
            .endTime(LocalDateTime.now().minusMinutes(10))
            .itemStatus(ItemStatus.USED_LIKE_NEW)
            .auctionStatus(AuctionStatus.IN_PROGRESS)
            .deliveryInclude(true)
            .directDealAvailable(true)
            .region("서울시")
            .preferredPlace("강남역")
            .build();
        ReflectionTestUtils.setField(endedItemWithBids, "id", 1L);

        endedItemNoBids = DelayedItem.builder()
            .sellerUserId(1L)
            .name("입찰 없는 경매")
            .category(Category.ELECTRONICS)
            .startPrice(10000L)
            .currentPrice(10000L)
            .endTime(LocalDateTime.now().minusMinutes(10))
            .itemStatus(ItemStatus.USED_LIKE_NEW)
            .auctionStatus(AuctionStatus.BEFORE_BIDDING)
            .deliveryInclude(true)
            .directDealAvailable(true)
            .region("서울시")
            .preferredPlace("강남역")
            .build();
        ReflectionTestUtils.setField(endedItemNoBids, "id", 2L);

        mockDeal = DelayedDeal.builder()
            .item(endedItemWithBids)
            .buyer(mockBuyer)
            .winningPrice(15000L)
            .status(DealStatus.PAID)
            .build();
        ReflectionTestUtils.setField(mockDeal, "id", 1L);
    }

    @Test
    @DisplayName("낙찰 처리 성공 - 입찰이 있는 경우")
    void t1() {
        // given
        when(delayedItemRepository.findByIdWithLock(1L))
            .thenReturn(Optional.of(endedItemWithBids));
        when(delayedDealService.hasBidsForAuction(1L))
            .thenReturn(true);
        when(delayedDealService.createDealFromAuction(1L))
            .thenReturn(mockDeal);

        // when
        delayedAuctionProcessor.processEndedAuction(1L);

        // then
        // 1. 상태 변경 확인
        assertThat(endedItemWithBids.getAuctionStatus()).isEqualTo(AuctionStatus.IN_DEAL);

        // 2. Deal 생성 메서드 호출 확인
        verify(delayedDealService).createDealFromAuction(1L);

        // 3. 이벤트 발행 확인
        ArgumentCaptor<DelayedAuctionEndedEvent> eventCaptor =
            ArgumentCaptor.forClass(DelayedAuctionEndedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        DelayedAuctionEndedEvent event = eventCaptor.getValue();
        assertThat(event.delayedItemId()).isEqualTo(1L);
        assertThat(event.success()).isTrue();
        assertThat(event.winnerUserId()).isEqualTo(2L);
        assertThat(event.finalPrice()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("유찰 처리 성공 - 입찰이 없는 경우")
    void t2() {
        // given
        when(delayedItemRepository.findByIdWithLock(2L))
            .thenReturn(Optional.of(endedItemNoBids));
        when(delayedDealService.hasBidsForAuction(2L))
            .thenReturn(false);

        // when
        delayedAuctionProcessor.processEndedAuction(2L);

        // then
        // 1. 상태 변경 확인
        assertThat(endedItemNoBids.getAuctionStatus()).isEqualTo(AuctionStatus.FAILED);

        // 2. Deal 생성 안됨
        verify(delayedDealService, never()).createDealFromAuction(anyLong());

        // 3. 이벤트 발행 확인 (유찰)
        ArgumentCaptor<DelayedAuctionEndedEvent> eventCaptor =
            ArgumentCaptor.forClass(DelayedAuctionEndedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        DelayedAuctionEndedEvent event = eventCaptor.getValue();
        assertThat(event.success()).isFalse();
        assertThat(event.winnerUserId()).isNull();
    }

    @Test
    @DisplayName("이미 처리된 경매 - 처리 스킵")
    void t3() {
        // given - 이미 IN_DEAL 상태
        DelayedItem alreadyProcessed = DelayedItem.builder()
            .sellerUserId(1L)
            .name("이미 처리된 경매")
            .auctionStatus(AuctionStatus.IN_DEAL)  // ✅ 이미 처리됨
            .endTime(LocalDateTime.now().minusMinutes(10))
            .build();

        when(delayedItemRepository.findByIdWithLock(3L))
            .thenReturn(Optional.of(alreadyProcessed));

        // when
        delayedAuctionProcessor.processEndedAuction(3L);

        // then - 아무 것도 안 함
        verify(delayedDealService, never()).hasBidsForAuction(anyLong());
        verify(delayedDealService, never()).createDealFromAuction(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("종료 시간이 안 된 경매 - 처리 스킵")
    void t4() {
        // given - 아직 종료 안됨
        DelayedItem notEnded = DelayedItem.builder()
            .sellerUserId(1L)
            .name("진행중인 경매")
            .auctionStatus(AuctionStatus.IN_PROGRESS)
            .endTime(LocalDateTime.now().plusDays(1))  // ✅ 미래
            .build();

        when(delayedItemRepository.findByIdWithLock(4L))
            .thenReturn(Optional.of(notEnded));

        // when
        delayedAuctionProcessor.processEndedAuction(4L);

        // then
        verify(delayedDealService, never()).hasBidsForAuction(anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 아이템 - 처리 스킵")
    void t5() {
        // given
        when(delayedItemRepository.findByIdWithLock(99999L))
            .thenReturn(Optional.empty());

        // when
        delayedAuctionProcessor.processEndedAuction(99999L);

        // then
        verify(delayedDealService, never()).hasBidsForAuction(anyLong());
    }
}
