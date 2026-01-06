package devut.buzzerbidder.domain.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import devut.buzzerbidder.domain.auctionroom.event.AuctionRoomStartedEvent;
import devut.buzzerbidder.domain.likelive.repository.LikeLiveRepository;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class AuctionRoomNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private LikeLiveRepository likeLiveRepository;

    @Mock
    private LiveItemRepository liveItemRepository;

    @InjectMocks
    private AuctionRoomNotificationListener listener;

    @Test
    @DisplayName("t1: 한 유저가 1개 상품만 찜한 경우 - 단일 상품 알림")
    void t1() {
        // given
        Long roomId = 1L;
        Long itemId = 100L;
        Long userId = 1L;
        LocalDateTime liveTime = LocalDateTime.of(2025, 12, 25, 14, 0);

        LiveItem item = LiveItem.builder()
            .name("테스트 상품")
            .build();

        given(liveItemRepository.findById(itemId)).willReturn(Optional.of(item));
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId)).willReturn(List.of(userId));

        AuctionRoomStartedEvent event = new AuctionRoomStartedEvent(
            roomId,
            liveTime,
            List.of(itemId)
        );

        // when
        listener.handleAuctionRoomStarted(event);

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).createAndSend(
            eq(userId),
            eq(NotificationType.LIVE_AUCTION_START),
            messageCaptor.capture(),
            eq("AUCTION_ROOM"),
            eq(roomId),
            any(Map.class)
        );

        String capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage).isEqualTo("찜한 '테스트 상품' 상품의 라이브 경매가 곧 시작합니다.");
    }

    @Test
    @DisplayName("t2: 한 유저가 여러 개 상품을 찜한 경우 - 통합 알림")
    void t2() {
        // given
        Long roomId = 1L;
        Long itemId1 = 100L;
        Long itemId2 = 200L;
        Long itemId3 = 300L;
        Long userId = 1L;
        LocalDateTime liveTime = LocalDateTime.of(2025, 12, 25, 14, 0);

        LiveItem item1 = LiveItem.builder().name("iPhone 15").build();
        LiveItem item2 = LiveItem.builder().name("MacBook Pro").build();
        LiveItem item3 = LiveItem.builder().name("AirPods").build();

        given(liveItemRepository.findById(itemId1)).willReturn(Optional.of(item1));
        given(liveItemRepository.findById(itemId2)).willReturn(Optional.of(item2));
        given(liveItemRepository.findById(itemId3)).willReturn(Optional.of(item3));

        // 모든 상품을 같은 유저가 찜함
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId1)).willReturn(List.of(userId));
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId2)).willReturn(List.of(userId));
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId3)).willReturn(List.of(userId));

        AuctionRoomStartedEvent event = new AuctionRoomStartedEvent(
            roomId,
            liveTime,
            List.of(itemId1, itemId2, itemId3)
        );

        // when
        listener.handleAuctionRoomStarted(event);

        // then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).createAndSend(
            eq(userId),
            eq(NotificationType.LIVE_AUCTION_START),
            messageCaptor.capture(),
            eq("AUCTION_ROOM"),
            eq(roomId),
            any(Map.class)
        );

        String capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage).contains("외 2개 상품");
    }

    @Test
    @DisplayName("t3: 여러 유저가 다른 상품들을 찜한 경우 - 각 유저에게 개별 통합 알림")
    void t3() {
        // given
        Long roomId = 1L;
        Long itemId1 = 100L;
        Long itemId2 = 200L;
        LocalDateTime liveTime = LocalDateTime.of(2025, 12, 25, 14, 0);

        LiveItem item1 = LiveItem.builder().name("상품1").build();
        LiveItem item2 = LiveItem.builder().name("상품2").build();

        given(liveItemRepository.findById(itemId1)).willReturn(Optional.of(item1));
        given(liveItemRepository.findById(itemId2)).willReturn(Optional.of(item2));

        // 유저1: 상품1, 상품2 모두 찜
        // 유저2: 상품1만 찜
        // 유저3: 상품2만 찜
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId1)).willReturn(List.of(1L, 2L));
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId2)).willReturn(List.of(1L, 3L));

        AuctionRoomStartedEvent event = new AuctionRoomStartedEvent(
            roomId,
            liveTime,
            List.of(itemId1, itemId2)
        );

        // when
        listener.handleAuctionRoomStarted(event);

        // then
        // 유저1: 2개 상품 찜 → 통합 알림
        // 유저2: 1개 상품 찜 → 단일 알림
        // 유저3: 1개 상품 찜 → 단일 알림
        verify(notificationService, times(3)).createAndSend(
            anyLong(),
            eq(NotificationType.LIVE_AUCTION_START),
            any(String.class),
            eq("AUCTION_ROOM"),
            eq(roomId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t4: 상품이 존재하지 않으면 해당 상품은 스킵하고 다른 상품은 처리")
    void t4() {
        // given
        Long roomId = 1L;
        Long itemId1 = 100L;
        Long itemId2 = 200L;
        Long userId = 1L;
        LocalDateTime liveTime = LocalDateTime.of(2025, 12, 25, 14, 0);

        LiveItem item2 = LiveItem.builder().name("상품2").build();

        given(liveItemRepository.findById(itemId1)).willReturn(Optional.empty()); // 상품1 없음
        given(liveItemRepository.findById(itemId2)).willReturn(Optional.of(item2));
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId2)).willReturn(List.of(userId));

        AuctionRoomStartedEvent event = new AuctionRoomStartedEvent(
            roomId,
            liveTime,
            List.of(itemId1, itemId2)
        );

        // when
        listener.handleAuctionRoomStarted(event);

        // then
        // 상품2만 처리됨
        verify(notificationService, times(1)).createAndSend(
            eq(userId),
            eq(NotificationType.LIVE_AUCTION_START),
            any(String.class),
            eq("AUCTION_ROOM"),
            eq(roomId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t5: 찜한 유저가 아예 없으면 알림을 보내지 않는다")
    void t5() {
        // given
        Long roomId = 1L;
        Long itemId = 100L;
        LocalDateTime liveTime = LocalDateTime.of(2025, 12, 25, 14, 0);

        LiveItem item = LiveItem.builder()
            .name("테스트 상품")
            .build();

        given(liveItemRepository.findById(itemId)).willReturn(Optional.of(item));
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId))
            .willReturn(Collections.emptyList());

        AuctionRoomStartedEvent event = new AuctionRoomStartedEvent(
            roomId,
            liveTime,
            List.of(itemId)
        );

        // when
        listener.handleAuctionRoomStarted(event);

        // then
        verify(notificationService, never()).createAndSend(
            anyLong(), any(), any(), any(), anyLong(), any()
        );
    }

    @Test
    @DisplayName("t6: 여러 상품이 있지만 모두 찜한 유저가 없으면 알림 없음")
    void t6() {
        // given
        Long roomId = 1L;
        Long itemId1 = 100L;
        Long itemId2 = 200L;
        LocalDateTime liveTime = LocalDateTime.of(2025, 12, 25, 14, 0);

        LiveItem item1 = LiveItem.builder().name("상품1").build();
        LiveItem item2 = LiveItem.builder().name("상품2").build();

        given(liveItemRepository.findById(itemId1)).willReturn(Optional.of(item1));
        given(liveItemRepository.findById(itemId2)).willReturn(Optional.of(item2));
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId1)).willReturn(Collections.emptyList());
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId2)).willReturn(Collections.emptyList());

        AuctionRoomStartedEvent event = new AuctionRoomStartedEvent(
            roomId,
            liveTime,
            List.of(itemId1, itemId2)
        );

        // when
        listener.handleAuctionRoomStarted(event);

        // then
        verify(notificationService, never()).createAndSend(
            anyLong(), any(), any(), any(), anyLong(), any()
        );
    }
}