package devut.buzzerbidder.global.notification.listener;

import devut.buzzerbidder.domain.deal.event.DelayedAuctionEndedEvent;
import devut.buzzerbidder.global.notification.enums.NotificationType;
import devut.buzzerbidder.global.notification.service.NotificationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DelayedAuctionEndedNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DelayedAuctionEndedEvent event) {

        if (event.success()) {
            // 1. 판매자 - 낙찰 알림
            notificationService.createAndSend(
                event.sellerUsedId(),
                NotificationType.DELAYED_SUCCESS_SELLER,
                "%s 상품이 낙찰되었습니다.".formatted(event.delayedItemName()),
                "DELAYED_ITEM",
                event.delayedItemId(),
                Map.of(
                    "finalPrice", event.finalPrice(),
                    "winnerUserId", event.winnerUserId()
                )
            );

            // 2. 낙찰자(입찰자) - 낙찰 알림
            notificationService.createAndSend(
                event.winnerUserId(),
                NotificationType.DELAYED_SUCCESS_BIDDER,
                "축하합니다! %s 상품의 낙찰에 성공했습니다.".formatted(event.delayedItemName()),
                "DELAYED_ITEM",
                event.delayedItemId(),
                Map.of(
                    "finalPrice", event.finalPrice()
                )
            );
        } else {
            // 3. 판매자 - 유찰 알림
            notificationService.createAndSend(
                event.sellerUsedId(),
                NotificationType.DELAYED_FAILED_SELLER,
                "%s 상품이 유찰되었습니다.".formatted(event.delayedItemName()),
                "DELAYED_ITEM",
                event.delayedItemId(),
                null
            );
        }
    }

}
