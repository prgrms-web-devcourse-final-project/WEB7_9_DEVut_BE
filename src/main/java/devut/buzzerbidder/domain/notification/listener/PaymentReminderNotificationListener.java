package devut.buzzerbidder.domain.notification.listener;

import devut.buzzerbidder.domain.deal.event.PaymentReminderEvent;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.service.NotificationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentReminderNotificationListener {

    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentReminder(PaymentReminderEvent event) {

        String message = "'%s 상품의 잔금 결제 기한이 %d시간 남았습니다."
            .formatted(event.itemName(), event.remainingHours());

        notificationService.createAndSend(
            event.buyerId(),
            NotificationType.PAYMENT_REMINDER,
            message,
            "LIVE_DEAL",
            event.dealId(),
            Map.of(
                "itemName", event.itemName(),
                "finalPrice", event.finalPrice(),
                "timeMarker", event.remainingHours() + "h"
            )
        );
    }
}
