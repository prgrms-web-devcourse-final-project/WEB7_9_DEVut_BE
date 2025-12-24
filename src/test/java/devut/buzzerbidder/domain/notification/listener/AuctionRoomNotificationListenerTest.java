package devut.buzzerbidder.domain.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
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
    @DisplayName("t1: 경매방 시작시 찜한 유저들에게 알림 전송")
    void t1() {
        // given
        Long roomId = 1L;
        Long itemId = 100L;
        LocalDateTime liveTime = LocalDateTime.of(2025, 12, 25, 14, 0);

        LiveItem item = LiveItem.builder()
            .name("테스트 상품")
            .build();

        List<Long> likeUserIds = List.of(1L, 2L, 3L);

        given(liveItemRepository.findById(itemId)).willReturn(Optional.of(item));
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId)).willReturn(likeUserIds);

        AuctionRoomStartedEvent event = new AuctionRoomStartedEvent(
            roomId,
            liveTime,
            List.of(itemId)
        );

        // when
        listener.handleAuctionRoomStarted(event);

        // then
        verify(notificationService, times(3)).createAndSend(
            anyLong(),
            eq(NotificationType.LIVE_AUCTION_START),
            contains("테스트 상품"),
            eq("AUCTION_ROOM"),
            eq(roomId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t2: 상품이 존재하지 않으면 알림을 스킵한다.")
    void t2() {
        // given
        Long roomId = 1L;
        Long itemId = 100L;
        LocalDateTime liveTime = LocalDateTime.of(2025, 12, 25, 14, 0);

        given(liveItemRepository.findById(itemId)).willReturn(Optional.empty());

        AuctionRoomStartedEvent event = new AuctionRoomStartedEvent(
            roomId,
            liveTime,
            List.of(itemId)
        );

        // when
        listener.handleAuctionRoomStarted(event);

        // then
        verify(likeLiveRepository, never()).findUserIdsByLiveItemId(anyLong());
        verify(notificationService, never()).createAndSend(
            anyLong(), any(), any(), any(), anyLong(), any()
        );
    }

    @Test
    @DisplayName("t3: 찜한 유저가 없으면 알림을 보내지 않는다.")
    void t3() {
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
    @DisplayName("t4: 여러 상품이 있을 때 각 상품의 찜한 유저들에게 알림 전송")
    void t4() {
        // given
        Long roomId = 1L;
        Long itemId1 = 100L;
        Long itemId2 = 200L;
        LocalDateTime liveTime = LocalDateTime.of(2025, 12, 25, 14, 0);

        LiveItem item1 = LiveItem.builder().name("상품1").build();
        LiveItem item2 = LiveItem.builder().name("상품2").build();

        given(liveItemRepository.findById(itemId1)).willReturn(Optional.of(item1));
        given(liveItemRepository.findById(itemId2)).willReturn(Optional.of(item2));
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId1)).willReturn(List.of(1L, 2L));
        given(likeLiveRepository.findUserIdsByLiveItemId(itemId2)).willReturn(List.of(3L));

        AuctionRoomStartedEvent event = new AuctionRoomStartedEvent(
            roomId,
            liveTime,
            List.of(itemId1, itemId2)
        );

        // when
        listener.handleAuctionRoomStarted(event);

        // then
        verify(notificationService, times(3)).createAndSend(
            anyLong(),
            eq(NotificationType.LIVE_AUCTION_START),
            any(),
            eq("AUCTION_ROOM"),
            eq(roomId),
            any(Map.class)
        );
    }
}
