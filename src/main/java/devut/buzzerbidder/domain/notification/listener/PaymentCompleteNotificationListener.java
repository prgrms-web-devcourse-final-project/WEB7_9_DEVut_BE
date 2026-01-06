package devut.buzzerbidder.domain.notification.listener;

import devut.buzzerbidder.domain.deal.event.PaymentCompleteEvent;
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
public class PaymentCompleteNotificationListener {

    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentComplete(PaymentCompleteEvent event) {

        // 판매자에게 알림
        notificationService.createAndSend(
            event.sellerId(),
            NotificationType.PAYMENT_COMPLETE,
            "'%s' 상품의 잔금이 입금되었습니다. 상품을 발송해주세요."
                .formatted(event.itemName()),
            "LIVE_DEAL",
            event.dealId(),
            Map.of(
                "itemName", event.itemName(),
                "totalPrice", event.totalPrice(),
                "remainingAmount", event.remainingAmount()
            )
        );
    }

}
