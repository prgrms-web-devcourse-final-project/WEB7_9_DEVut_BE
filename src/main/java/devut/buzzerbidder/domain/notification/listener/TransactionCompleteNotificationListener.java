package devut.buzzerbidder.domain.notification.listener;

import devut.buzzerbidder.domain.deal.enums.AuctionType;
import devut.buzzerbidder.domain.deal.event.TransactionCompleteEvent;
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
public class TransactionCompleteNotificationListener {

    private final NotificationService notificationService;

    @Async("notificationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionComplete(TransactionCompleteEvent event) {
        // 판매자에게 거래 완료 알림
        notificationService.createAndSend(
            event.sellerId(),
            NotificationType.TRANSACTION_COMPLETE,
            "'%s' 상품의 거래가 완료되었습니다.".formatted(event.itemName()),
            event.auctionType() == AuctionType.DELAYED ? "DELAYED_DEAL" : "LIVE_DEAL",
            event.dealId(),
            Map.of(
                "itemName", event.itemName(),
                "finalPrice", event.finalPrice(),
                "buyerId", event.buyerId()
            )
        );
    }

}
