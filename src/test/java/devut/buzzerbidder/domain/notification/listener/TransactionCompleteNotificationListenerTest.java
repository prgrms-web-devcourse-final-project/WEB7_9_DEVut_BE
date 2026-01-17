package devut.buzzerbidder.domain.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import devut.buzzerbidder.domain.deal.enums.AuctionType;
import devut.buzzerbidder.domain.deal.event.TransactionCompleteEvent;
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
class TransactionCompleteNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransactionCompleteNotificationListener listener;

    @Test
    @DisplayName("t1: 거래 완료시 판매자에게 알림 전송")
    void t1() {
        // given
        Long dealId = 1L;
        Long buyerId = 10L;
        Long sellerId = 20L;
        Long itemId = 100L;
        String itemName = "테스트 상품";
        Long finalPrice = 100000L;

        TransactionCompleteEvent event = new TransactionCompleteEvent(
            dealId, buyerId, sellerId, itemId, AuctionType.LIVE, itemName, finalPrice
        );

        // when
        listener.handleTransactionComplete(event);

        // then
        verify(notificationService).createAndSend(
            eq(sellerId),
            eq(NotificationType.TRANSACTION_COMPLETE),
            contains("거래가 완료되었습니다"),
            eq("LIVE_DEAL"),
            eq(dealId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t2: 알림 메시지에 상품명이 포함됨")
    void t2() {
        // given
        String itemName = "특별한 상품";

        TransactionCompleteEvent event = new TransactionCompleteEvent(
            1L, 10L, 20L, 100L, AuctionType.LIVE, itemName, 100000L
        );

        // when
        listener.handleTransactionComplete(event);

        // then
        verify(notificationService).createAndSend(
            eq(20L),
            eq(NotificationType.TRANSACTION_COMPLETE),
            contains(itemName),
            eq("LIVE_DEAL"),
            eq(1L),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t3: 라이브 경매일 경우 LIVE_DEAL 타입으로 알림 전송")
    void t3() {
        // given
        TransactionCompleteEvent event = new TransactionCompleteEvent(
            1L, 10L, 20L, 100L, AuctionType.LIVE, "상품", 100000L
        );

        // when
        listener.handleTransactionComplete(event);

        // then
        verify(notificationService).createAndSend(
            eq(20L),
            eq(NotificationType.TRANSACTION_COMPLETE),
            any(String.class),
            eq("LIVE_DEAL"),
            eq(1L),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t4: 지연 경매일 경우 DELAYED_DEAL 타입으로 알림 전송")
    void t4() {
        // given
        TransactionCompleteEvent event = new TransactionCompleteEvent(
            1L, 10L, 20L, 100L, AuctionType.DELAYED, "상품", 100000L
        );

        // when
        listener.handleTransactionComplete(event);

        // then
        verify(notificationService).createAndSend(
            eq(20L),
            eq(NotificationType.TRANSACTION_COMPLETE),
            any(String.class),
            eq("DELAYED_DEAL"),
            eq(1L),
            any(Map.class)
        );
    }
}
