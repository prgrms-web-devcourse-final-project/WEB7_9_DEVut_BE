package devut.buzzerbidder.domain.notification.listener;

import devut.buzzerbidder.domain.delayedbid.event.DelayedBidOutbidEvent;
import devut.buzzerbidder.domain.delayedbid.event.DelayedBuyNowEvent;
import devut.buzzerbidder.domain.delayedbid.event.DelayedFirstBidEvent;
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
public class DelayedBidNotificationListener {

    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFirstBid(DelayedFirstBidEvent event) {

        String message = "'%s' 상품에 첫 입찰(₩%,d)이 들어왔습니다!"
            .formatted(event.delayedItemName(), event.firstBidAmount());

        notificationService.createAndSend(
            event.sellerUserId(),
            NotificationType.DELAYED_FIRST_BID,
            message,
            "DELAYED_ITEM",
            event.delayedItemId(),
            Map.of(
                "firstBidderUserId", event.firstBidderUserId(),
                "firstBidAmount", event.firstBidAmount()
            )
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOutbid(DelayedBidOutbidEvent event)  {

        String message = "'%s' 상품 상위 입찰이 들어왔습니다."
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBuyNow(DelayedBuyNowEvent event) {

        // 1. 판매자 알림
        notificationService.createAndSend(
            event.sellerUserId(),
            NotificationType.DELAYED_BUY_NOW_SOLD,
            "'%s' 상품이 즉시 구매로 판매되었습니다."
                .formatted(event.delayedItemName()),
            "DELAYED_ITEM",
            event.delayedItemId(),
            Map.of(
                "buyNowPrice", event.buyNowPrice(),
                "buyUserId", event.buyUserId()
            )
        );

        // 2. 기존 최고 입찰자 알림 (있을 때만)
        if (event.previousHighestBidderUserId() != null) {
            notificationService.createAndSend(
                event.previousHighestBidderUserId(),
                NotificationType.DELAYED_CANCELLED_BY_BUY_NOW,
                "'%s' 상품이 즉시 구매로 종료되었습니다."
                    .formatted(event.delayedItemName()),
                "DELAYED_ITEM",
                event.delayedItemId(),
                Map.of(
                    "buyNowPrice", event.buyNowPrice()
                )
            );
        }
    }
}
