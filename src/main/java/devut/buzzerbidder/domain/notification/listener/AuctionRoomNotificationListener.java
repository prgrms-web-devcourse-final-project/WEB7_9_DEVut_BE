package devut.buzzerbidder.domain.notification.listener;

import devut.buzzerbidder.domain.auctionroom.event.AuctionRoomStartedEvent;
import devut.buzzerbidder.domain.likelive.repository.LikeLiveRepository;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionRoomNotificationListener {

    private final NotificationService notificationService;
    private final LikeLiveRepository likeLiveRepository;
    private final LiveItemRepository liveItemRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuctionRoomStarted(AuctionRoomStartedEvent event) {
        // 경매방의 각 상품을 찜한 유저들에게 알림
        for (Long itemId : event.liveItemIds()) {

            LiveItem item = liveItemRepository.findById(itemId)
                .orElse(null);

            if (item == null) continue;

            // 해당 상품을 찜한 유저 목록 조회
            List<Long> likeUserIds = likeLiveRepository.findUserIdsByLiveItemId(itemId);

            if (likeUserIds.isEmpty()) continue;

            // 찜한 유저들에게 벌크 알림 발송
            notificationService.createAndSendToMultiple(
                likeUserIds,
                NotificationType.LIVE_AUCTION_START,
                "찜한 '%s' 상품의 라이브 경매가 곧 시작합니다.".formatted(item.getName()),
                "AUCTION_ROOM",
                event.roomId(),
                Map.of(
                    "liveItemId", itemId,
                    "liveTime", event.liveTime().toString()
                )
            );
        }
    }

}
