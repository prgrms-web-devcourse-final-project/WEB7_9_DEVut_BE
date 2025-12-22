package devut.buzzerbidder.global.notification.listener;

import devut.buzzerbidder.domain.delayedbid.event.DelayedBidOutbidEvent;
import devut.buzzerbidder.global.notification.enums.NotificationType;
import devut.buzzerbidder.global.notification.service.NotificationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DelayedBidNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DelayedBidOutbidEvent event) {

        String message = "%s 상품 상위 입찰이 들어왔습니다."
            .formatted(event.delayedItemName());

        notificationService.createAndSend(
            event.previousBidderUserId(),
            NotificationType.DELAYED_BID_OUTBID,
            message,
            "DELAYED_ITEM",
            event.delayedItemId(),
            Map.of(
                "newBidAmount", event.newBidAmount(),
                "newBidderUserId", event.newBidderUserId()
            )
        );
    }
}
