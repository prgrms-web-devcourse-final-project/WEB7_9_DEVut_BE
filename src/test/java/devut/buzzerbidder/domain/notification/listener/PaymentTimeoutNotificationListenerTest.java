package devut.buzzerbidder.domain.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import devut.buzzerbidder.domain.deal.event.PaymentTimeoutEvent;
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
class PaymentTimeoutNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentTimeoutNotificationListener listener;

    @Test
    @DisplayName("t1: 결제 기한 초과시 구매자에게 거래 취소 알림 전송")
    void t1() {
        // given
        Long dealId = 1L;
        Long buyerId = 10L;
        Long sellerId = 20L;
        Long itemId = 100L;
        String itemName = "테스트 상품";
        Long finalPrice = 100000L;

        PaymentTimeoutEvent event = new PaymentTimeoutEvent(
            dealId, buyerId, sellerId, itemId, itemName, finalPrice
        );

        // when
        listener.handlePaymentTimeout(event);

        // then
        verify(notificationService).createAndSend(
            eq(buyerId),
            eq(NotificationType.PAYMENT_TIMEOUT_BUYER),
            contains("거래가 취소되었습니다"),
            eq("LIVE_DEAL"),
            eq(dealId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t2: 결제 기한 초과시 판매자에게 거래 취소 알림 전송")
    void t2() {
        // given
        Long dealId = 1L;
        Long buyerId = 10L;
        Long sellerId = 20L;
        Long itemId = 100L;
        String itemName = "테스트 상품";
        Long finalPrice = 100000L;

        PaymentTimeoutEvent event = new PaymentTimeoutEvent(
            dealId, buyerId, sellerId, itemId, itemName, finalPrice
        );

        // when
        listener.handlePaymentTimeout(event);

        // then
        verify(notificationService).createAndSend(
            eq(sellerId),
            eq(NotificationType.PAYMENT_TIMEOUT_SELLER),
            contains("거래가 취소되었습니다"),
            eq("LIVE_DEAL"),
            eq(dealId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t3: 결제 기한 초과시 구매자와 판매자 모두에게 알림 전송")
    void t3() {
        // given
        PaymentTimeoutEvent event = new PaymentTimeoutEvent(
            1L, 10L, 20L, 100L, "상품", 100000L
        );

        // when
        listener.handlePaymentTimeout(event);

        // then
        verify(notificationService, times(2)).createAndSend(
            any(Long.class),
            any(NotificationType.class),
            any(String.class),
            eq("LIVE_DEAL"),
            eq(1L),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t4: 구매자 알림에 보증금 환불 불가 안내가 포함됨")
    void t4() {
        // given
        PaymentTimeoutEvent event = new PaymentTimeoutEvent(
            1L, 10L, 20L, 100L, "상품", 100000L
        );

        // when
        listener.handlePaymentTimeout(event);

        // then
        verify(notificationService).createAndSend(
            eq(10L),
            eq(NotificationType.PAYMENT_TIMEOUT_BUYER),
            contains("환불되지 않습니다"),
            eq("LIVE_DEAL"),
            eq(1L),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t5: 알림 메시지에 상품명이 포함됨")
    void t5() {
        // given
        String itemName = "특별한 상품";

        PaymentTimeoutEvent event = new PaymentTimeoutEvent(
            1L, 10L, 20L, 100L, itemName, 100000L
        );

        // when
        listener.handlePaymentTimeout(event);

        // then
        verify(notificationService).createAndSend(
            eq(10L),
            eq(NotificationType.PAYMENT_TIMEOUT_BUYER),
            contains(itemName),
            eq("LIVE_DEAL"),
            eq(1L),
            any(Map.class)
        );
    }
}
