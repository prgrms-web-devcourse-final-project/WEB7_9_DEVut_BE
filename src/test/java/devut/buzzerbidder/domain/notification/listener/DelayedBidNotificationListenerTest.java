package devut.buzzerbidder.domain.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import devut.buzzerbidder.domain.delayedbid.event.DelayedBidOutbidEvent;
import devut.buzzerbidder.domain.delayedbid.event.DelayedBuyNowEvent;
import devut.buzzerbidder.domain.delayedbid.event.DelayedFirstBidEvent;
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
public class DelayedBidNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DelayedBidNotificationListener listener;

    // ========== 첫 입찰 알림 테스트 ==========

    @Test
    @DisplayName("t1: 첫 입찰시 판매자에게 알림 전송")
    void t1() {
        // given
        Long delayedItemId = 1L;
        String itemName = "테스트 상품";
        Long sellerId = 10L;
        Long bidderId = 20L;
        Long bidAmount = 50000L;

        DelayedFirstBidEvent event = new DelayedFirstBidEvent(
            delayedItemId,
            itemName,
            sellerId,
            bidderId,
            bidAmount
        );

        // when
        listener.handleFirstBid(event);

        // then
        verify(notificationService).createAndSend(
            eq(sellerId),
            eq(NotificationType.DELAYED_FIRST_BID),
            contains("첫 입찰"),
            eq("DELAYED_ITEM"),
            eq(delayedItemId),
            any(Map.class)
        );
    }

    // ========== 입찰가 밀림 테스트 ==========

    @Test
    @DisplayName("t2: 입찰가 밀렸을 때 이전 입찰자에게 알림 전송")
    void t2() {
        // given
        Long delayedItemId = 1L;
        String itemName = "테스트 상품";
        Long previousBidderId = 10L;
        Long newBidderId = 20L;
        Long newBidAmount = 50000L;

        DelayedBidOutbidEvent event = new DelayedBidOutbidEvent(
            delayedItemId,
            itemName,
            previousBidderId,
            newBidderId,
            newBidAmount
        );

        // when
        listener.handleOutbid(event);

        // then
        verify(notificationService).createAndSend(
            eq(previousBidderId),
            eq(NotificationType.DELAYED_BID_OUTBID),
            contains("상위 입찰이 들어왔습니다"),
            eq("DELAYED_ITEM"),
            eq(delayedItemId),
            any(Map.class)
        );
    }

    // ========== 즉시 구매 테스트 ==========

    @Test
    @DisplayName("t3: 즉시 구매시 판매자에게 알림 전송")
    void t3() {
        // given
        Long delayedItemId = 1L;
        String itemName = "테스트 상품";
        Long buyUserId = 30L;
        Long sellerId = 10L;
        Long previousBidderId = 20L;
        Long buyNowPrice = 100000L;

        DelayedBuyNowEvent event = new DelayedBuyNowEvent(
            delayedItemId,
            itemName,
            buyUserId,
            sellerId,
            previousBidderId,
            buyNowPrice
        );

        // when
        listener.handleBuyNow(event);

        // then
        verify(notificationService).createAndSend(
            eq(sellerId),
            eq(NotificationType.DELAYED_BUY_NOW_SOLD),
            contains("즉시 구매로 판매되었습니다"),
            eq("DELAYED_ITEM"),
            eq(delayedItemId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t4: 즉시 구매시 기존 최고 입찰자에게 알림 전송")
    void t4() {
        // given
        Long delayedItemId = 1L;
        String itemName = "테스트 상품";
        Long buyUserId = 30L;
        Long sellerId = 10L;
        Long previousBidderId = 20L;
        Long buyNowPrice = 100000L;

        DelayedBuyNowEvent event = new DelayedBuyNowEvent(
            delayedItemId,
            itemName,
            buyUserId,
            sellerId,
            previousBidderId,
            buyNowPrice
        );

        // when
        listener.handleBuyNow(event);

        // then
        verify(notificationService).createAndSend(
            eq(previousBidderId),
            eq(NotificationType.DELAYED_CANCELLED_BY_BUY_NOW),
            contains("즉시 구매로 종료되었습니다"),
            eq("DELAYED_ITEM"),
            eq(delayedItemId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t5: 즉시 구매시 판매자와 기존 최고 입찰자 모두에게 알림 전송")
    void t5() {
        // given
        DelayedBuyNowEvent event = new DelayedBuyNowEvent(
            1L, "상품", 30L, 10L, 20L, 100000L
        );

        // when
        listener.handleBuyNow(event);

        // then
        verify(notificationService, times(2)).createAndSend(
            anyLong(), any(), any(), any(), anyLong(), any()
        );
    }

    @Test
    @DisplayName("t6: 기존 최고 입찰자가 없을 때 판매자에게만 알림 전송")
    void t6() {
        // given
        DelayedBuyNowEvent event = new DelayedBuyNowEvent(
            1L, "상품", 30L, 10L, null, 100000L
        );

        // when
        listener.handleBuyNow(event);

        // then
        verify(notificationService, times(1)).createAndSend(
            anyLong(), any(), any(), any(), anyLong(), any()
        );
    }
}
