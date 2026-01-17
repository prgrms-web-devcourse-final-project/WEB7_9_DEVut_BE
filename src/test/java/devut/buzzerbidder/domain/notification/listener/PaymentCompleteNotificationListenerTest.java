package devut.buzzerbidder.domain.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import devut.buzzerbidder.domain.deal.event.PaymentCompleteEvent;
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
public class PaymentCompleteNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentCompleteNotificationListener listener;

    @Test
    @DisplayName("t1: 잔금 결제 완료시 판매자에게 알림 전송")
    void t1() {
        // given
        Long dealId = 1L;
        Long buyerId = 10L;
        Long sellerId = 20L;
        Long itemId = 100L;
        String itemName = "테스트 상품";
        Long totalPrice = 100000L;
        Long depositAmount = 10000L;
        Long remainingAmount = 90000L;

        PaymentCompleteEvent event = new PaymentCompleteEvent(
            dealId,
            buyerId,
            sellerId,
            itemId,
            itemName,
            totalPrice,
            depositAmount,
            remainingAmount
        );

        // when
        listener.handlePaymentComplete(event);

        // then
        verify(notificationService).createAndSend(
            eq(sellerId),
            eq(NotificationType.PAYMENT_COMPLETE),
            contains("잔금이 입금되었습니다"),
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

        PaymentCompleteEvent event = new PaymentCompleteEvent(
            1L, 10L, 20L, 100L, itemName, 100000L, 10000L, 90000L
        );

        // when
        listener.handlePaymentComplete(event);

        // then
        verify(notificationService).createAndSend(
            eq(20L),
            eq(NotificationType.PAYMENT_COMPLETE),
            contains(itemName),
            eq("LIVE_DEAL"),
            eq(1L),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t3: 알림 메시지에 발송 요청 안내가 포함됨")
    void t3() {
        // given
        PaymentCompleteEvent event = new PaymentCompleteEvent(
            1L, 10L, 20L, 100L, "상품", 100000L, 10000L, 90000L
        );

        // when
        listener.handlePaymentComplete(event);

        // then
        verify(notificationService).createAndSend(
            eq(20L),
            eq(NotificationType.PAYMENT_COMPLETE),
            contains("발송해주세요"),
            eq("LIVE_DEAL"),
            eq(1L),
            any(Map.class)
        );
    }
}
