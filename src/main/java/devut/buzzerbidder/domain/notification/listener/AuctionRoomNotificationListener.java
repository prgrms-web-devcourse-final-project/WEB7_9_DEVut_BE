package devut.buzzerbidder.domain.notification.listener;

import devut.buzzerbidder.domain.auctionroom.event.AuctionRoomStartedEvent;
import devut.buzzerbidder.domain.likelive.repository.LikeLiveRepository;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.service.NotificationService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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

    @Async("notificationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuctionRoomStarted(AuctionRoomStartedEvent event) {
        // 1. 유저별로 찜한 상품 그룹화
        Map<Long, List<LiveItem>> userLikedItems = new HashMap<>();

        for (Long itemId : event.liveItemIds()) {
            LiveItem item = liveItemRepository.findById(itemId).orElse(null);
            if (item == null)
                continue;

            // 해당 상품을 찜한 유저 목록 조회
            List<Long> likeUserIds = likeLiveRepository.findUserIdsByLiveItemId(itemId);

            // 유저별로 상품 저장
            for (Long userId : likeUserIds) {
                userLikedItems.computeIfAbsent(userId, k -> new ArrayList<>()).add(item);
            }
        }

        // 2. 각 유저에게 통합 알림 발송
        for (Map.Entry<Long, List<LiveItem>> entry : userLikedItems.entrySet()) {
            Long userId = entry.getKey();
            List<LiveItem> items = entry.getValue();

            String message = createNotificationMessage(items);

            notificationService.createAndSend(
                userId,
                NotificationType.LIVE_AUCTION_START,
                message,
                "AUCTION_ROOM",
                event.roomId(),
                Map.of(
                    "itemName", items.get(0).getName(),
                    "itemCount", items.size(),
                    "itemsId", items.stream().map(LiveItem::getId).toList(),
                    "liveTime", event.liveTime().toString()
                )
            );
        }
    }

    private String createNotificationMessage(List<LiveItem> items) {
        if (items.size() == 1) {
            return "찜한 '%s' 상품의 라이브 경매가 곧 시작합니다.".formatted(items.get(0).getName());
        } else {
            return "찜한 '%s' 외 %d개 상품의 라이브 경매가 곧 시작합니다."
                .formatted(items.get(0).getName(), items.size() - 1);
        }
    }
}
