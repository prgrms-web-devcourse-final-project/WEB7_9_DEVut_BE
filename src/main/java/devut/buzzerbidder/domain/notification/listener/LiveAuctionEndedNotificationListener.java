package devut.buzzerbidder.domain.notification.listener;

import devut.buzzerbidder.domain.liveitem.event.LiveAuctionEndedEvent;
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
public class LiveAuctionEndedNotificationListener {

    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LiveAuctionEndedEvent event) {
        if (event.success()) {
            // 1. 판매자 - 낙찰 알림
            notificationService.createAndSend(
                event.sellerUserId(),
                NotificationType.LIVE_SUCCESS_SELLER,
                "'%s' 상품이 %d 원에 낙찰되었습니다.".formatted(event.liveItemName(), event.finalPrice()),
                "LIVE_ITEM",
                event.liveItemId(),
                Map.of(
                    "itemName", event.liveItemName(),
                    "finalPrice", event.finalPrice(),
                    "winnerUserId", event.winnerUserId()
                )
            );

            // 2. 낙찰자 - 낙찰 알림
            notificationService.createAndSend(
                event.winnerUserId(),
                NotificationType.LIVE_SUCCESS_BIDDER,
                "축하합니다! '%s' 상품의 낙찰에 성공했습니다.".formatted(event.liveItemName()),
                "LIVE_ITEM",
                event.liveItemId(),
                Map.of(
                    "itemName", event.liveItemName(),
                    "finalPrice", event.finalPrice()
                )
            );
        } else {
            // 3. 판매자 - 유찰 알림
            notificationService.createAndSend(
                event.sellerUserId(),
                NotificationType.LIVE_FAILED_SELLER,
                "'%s' 상품이 유찰되었습니다.".formatted(event.liveItemName()),
                "LIVE_ITEM",
                event.liveItemId(),
                Map.of(
                    "itemName", event.liveItemName()
                )
            );
        }
    }
}
