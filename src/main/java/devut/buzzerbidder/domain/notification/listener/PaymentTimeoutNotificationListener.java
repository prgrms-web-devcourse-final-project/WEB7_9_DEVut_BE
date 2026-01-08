package devut.buzzerbidder.domain.notification.listener;

import devut.buzzerbidder.domain.deal.event.PaymentTimeoutEvent;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.service.NotificationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentTimeoutNotificationListener {

    private final NotificationService notificationService;

    @Async("notificationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentTimeout(PaymentTimeoutEvent event) {

        // 구매자에게 알림
        notificationService.createAndSend(
            event.buyerId(),
            NotificationType.PAYMENT_TIMEOUT_BUYER,
            "'%s' 상품의 결제 기한이 초과되어 거래가 취소되었습니다. 보증금은 환불되지 않습니다."
                .formatted(event.itemName()),
            "LIVE_DEAL",
            event.dealId(),
            Map.of(
                "itemName", event.itemName(),
                "finalPrice", event.finalPrice()
            )
        );

        // 판매자에게 알림
        notificationService.createAndSend(
            event.sellerId(),
            NotificationType.PAYMENT_TIMEOUT_SELLER,
            "'%s' 상품의 구매자가 결제 기한 내에 잔금을 입금하지 않아 거래가 취소되었습니다."
                .formatted(event.itemName()),
            "LIVE_DEAL",
            event.dealId(),
            Map.of(
                "itemName", event.itemName(),
                "finalPrice", event.finalPrice()
            )
        );
    }

}
