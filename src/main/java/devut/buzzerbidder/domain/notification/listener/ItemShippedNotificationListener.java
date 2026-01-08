package devut.buzzerbidder.domain.notification.listener;

import devut.buzzerbidder.domain.deal.enums.AuctionType;
import devut.buzzerbidder.domain.deal.event.ItemShippedEvent;
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
public class ItemShippedNotificationListener {

    private final NotificationService notificationService;

    @Async("notificationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleItemShipped(ItemShippedEvent event) {

        String message = "'%s' 상품이 %s(으)로 발송되었습니다. (운송장: %s)"
            .formatted(event.itemName(), event.carrierName(), event.trackingNumber());

        notificationService.createAndSend(
            event.buyerId(),
            NotificationType.ITEM_SHIPPED,
            message,
            event.auctionType() == AuctionType.DELAYED ? "DELAYED_DEAL" : "LIVE_DEAL",
            event.dealId(),
            Map.of(
                "itemName", event.itemName(),
                "carrierName", event.carrierName(),
                "trackingNumber", event.trackingNumber()
            )
        );
    }
}
