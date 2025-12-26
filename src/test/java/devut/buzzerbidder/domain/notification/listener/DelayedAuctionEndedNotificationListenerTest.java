package devut.buzzerbidder.domain.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import devut.buzzerbidder.domain.deal.event.DelayedAuctionEndedEvent;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.service.NotificationService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DelayedAuctionEndedNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DelayedAuctionEndedNotificationListener listener;

    @Test
    @DisplayName("t1: 낙찰시 판매자에게 낙찰 알림 전송")
    void t1() {
        // given
        Long delayedItemId = 1L;
        String itemName = "테스트 상품";
        Long sellerId = 10L;
        Long winnerId = 20L;
        Long finalPrice = 50000L;

        DelayedAuctionEndedEvent event = new DelayedAuctionEndedEvent(
            delayedItemId,
            itemName,
            sellerId,
            true,
            winnerId,
            finalPrice
        );

        // when
        listener.handle(event);

        // then
        verify(notificationService).createAndSend(
            eq(sellerId),
            eq(NotificationType.DELAYED_SUCCESS_SELLER),
            contains("낙찰되었습니다"),
            eq("DELAYED_ITEM"),
            eq(delayedItemId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t2: 낙찰시 낙찰자에게 낙찰 알림 전송")
    void t2() {
        // given
        Long delayedItemId = 1L;
        String itemName = "테스트 상품";
        Long sellerId = 10L;
        Long winnerId = 20L;
        Long finalPrice = 50000L;

        DelayedAuctionEndedEvent event = new DelayedAuctionEndedEvent(
            delayedItemId,
            itemName,
            sellerId,
            true,
            winnerId,
            finalPrice
        );

        // when
        listener.handle(event);

        // then
        verify(notificationService).createAndSend(
            eq(winnerId),
            eq(NotificationType.DELAYED_SUCCESS_BIDDER),
            contains("낙찰에 성공했습니다"),
            eq("DELAYED_ITEM"),
            eq(delayedItemId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t3: 낙찰시 판매자와 낙찰자 모두에게 알림 전송")
    void t3() {
        // given
        DelayedAuctionEndedEvent event = new DelayedAuctionEndedEvent(
            1L, "상품", 10L, true, 20L, 50000L
        );

        // when
        listener.handle(event);

        // then
        verify(notificationService, times(2)).createAndSend(
            anyLong(), any(), any(), any(), anyLong(), any()
        );
    }

    @Test
    @DisplayName("t4: 유찰시 판매자에게만 유찰 알림 전송")
    void t4() {
        // given
        Long delayedItemId = 1L;
        String itemName = "유찰 상품";
        Long sellerId = 10L;

        DelayedAuctionEndedEvent event = new DelayedAuctionEndedEvent(
            delayedItemId,
            itemName,
            sellerId,
            false,
            null,
            null
        );

        // when
        listener.handle(event);

        // then
        verify(notificationService).createAndSend(
            eq(sellerId),
            eq(NotificationType.DELAYED_FAILED_SELLER),
            contains("유찰되었습니다"),
            eq("DELAYED_ITEM"),
            eq(delayedItemId),
            isNull()
        );
    }

    @Test
    @DisplayName("t5: 유찰시 한 번만 알림 전송 (판매자에게만)")
    void t5() {
        // given
        DelayedAuctionEndedEvent event = new DelayedAuctionEndedEvent(
            1L, "유찰 상품", 10L, false, null, null
        );

        // when
        listener.handle(event);

        // then
        verify(notificationService, times(1)).createAndSend(
            anyLong(), any(), any(), any(), anyLong(), any()
        );
    }
}
