package devut.buzzerbidder.domain.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import devut.buzzerbidder.domain.deal.enums.AuctionType;
import devut.buzzerbidder.domain.deal.event.ItemShippedEvent;
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
public class ItemShippedNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ItemShippedNotificationListener listener;

    // ========== 배송 시작 알림 테스트 ==========

    @Test
    @DisplayName("t1: 지연 경매 상품 발송시 구매자에게 알림 전송")
    void t1() {
        // given
        Long dealId = 1L;
        Long buyerId = 10L;
        Long sellerId = 20L;
        Long itemId = 100L;
        String itemName = "테스트 상품";
        String carrierName = "CJ대한통운";
        String trackingNumber = "123456789";

        ItemShippedEvent event = new ItemShippedEvent(
            dealId,
            buyerId,
            sellerId,
            itemId,
            AuctionType.DELAYED,
            itemName,
            carrierName,
            trackingNumber
        );

        // when
        listener.handleItemShipped(event);

        // then
        verify(notificationService).createAndSend(
            eq(buyerId),
            eq(NotificationType.ITEM_SHIPPED),
            contains("발송되었습니다"),
            eq("DELAYED_DEAL"),
            eq(dealId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t2: 라이브 경매 상품 발송시 구매자에게 알림 전송")
    void t2() {
        // given
        Long dealId = 2L;
        Long buyerId = 15L;
        Long sellerId = 25L;
        Long itemId = 200L;
        String itemName = "라이브 상품";
        String carrierName = "우체국택배";
        String trackingNumber = "987654321";

        ItemShippedEvent event = new ItemShippedEvent(
            dealId,
            buyerId,
            sellerId,
            itemId,
            AuctionType.LIVE,
            itemName,
            carrierName,
            trackingNumber
        );

        // when
        listener.handleItemShipped(event);

        // then
        verify(notificationService).createAndSend(
            eq(buyerId),
            eq(NotificationType.ITEM_SHIPPED),
            contains("발송되었습니다"),
            eq("LIVE_DEAL"),
            eq(dealId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t3: 알림 메시지에 상품명, 택배사, 송장번호가 포함됨")
    void t3() {
        // given
        String itemName = "특별한 상품";
        String carrierName = "로젠택배";
        String trackingNumber = "111222333";

        ItemShippedEvent event = new ItemShippedEvent(
            1L, 10L, 20L, 100L,
            AuctionType.DELAYED,
            itemName,
            carrierName,
            trackingNumber
        );

        // when
        listener.handleItemShipped(event);

        // then
        verify(notificationService).createAndSend(
            eq(10L),
            eq(NotificationType.ITEM_SHIPPED),
            contains(itemName),
            eq("DELAYED_DEAL"),
            eq(1L),
            any(Map.class)
        );

        verify(notificationService).createAndSend(
            eq(10L),
            eq(NotificationType.ITEM_SHIPPED),
            contains(carrierName),
            eq("DELAYED_DEAL"),
            eq(1L),
            any(Map.class)
        );

        verify(notificationService).createAndSend(
            eq(10L),
            eq(NotificationType.ITEM_SHIPPED),
            contains(trackingNumber),
            eq("DELAYED_DEAL"),
            eq(1L),
            any(Map.class)
        );
    }
}
