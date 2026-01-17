package devut.buzzerbidder.domain.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import devut.buzzerbidder.domain.deal.event.PaymentReminderEvent;
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
class PaymentReminderNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentReminderNotificationListener listener;

    @Test
    @DisplayName("t1: 결제 기한 임박시 구매자에게 알림 전송")
    void t1() {
        // given
        Long dealId = 1L;
        Long buyerId = 10L;
        Long itemId = 100L;
        String itemName = "테스트 상품";
        Long finalPrice = 100000L;
        int remainingHours = 24;

        PaymentReminderEvent event = new PaymentReminderEvent(
            dealId,
            buyerId,
            itemId,
            itemName,
            finalPrice,
            remainingHours
        );

        // when
        listener.handlePaymentReminder(event);

        // then
        verify(notificationService).createAndSend(
            eq(buyerId),
            eq(NotificationType.PAYMENT_REMINDER),
            contains("결제 기한"),
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

        PaymentReminderEvent event = new PaymentReminderEvent(
            1L, 10L, 100L, itemName, 100000L, 24
        );

        // when
        listener.handlePaymentReminder(event);

        // then
        verify(notificationService).createAndSend(
            eq(10L),
            eq(NotificationType.PAYMENT_REMINDER),
            contains(itemName),
            eq("LIVE_DEAL"),
            eq(1L),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t3: 알림 메시지에 남은 시간이 포함됨")
    void t3() {
        // given
        int remainingHours = 12;

        PaymentReminderEvent event = new PaymentReminderEvent(
            1L, 10L, 100L, "상품", 100000L, remainingHours
        );

        // when
        listener.handlePaymentReminder(event);

        // then
        verify(notificationService).createAndSend(
            eq(10L),
            eq(NotificationType.PAYMENT_REMINDER),
            contains(String.valueOf(remainingHours)),
            eq("LIVE_DEAL"),
            eq(1L),
            any(Map.class)
        );
    }
}
