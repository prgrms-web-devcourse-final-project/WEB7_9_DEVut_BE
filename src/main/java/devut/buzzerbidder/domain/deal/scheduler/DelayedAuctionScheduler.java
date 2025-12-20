package devut.buzzerbidder.domain.deal.scheduler;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.AuctionStatus;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DelayedAuctionScheduler {

    private final DelayedItemRepository delayedItemRepository;
    private final DelayedAuctionProcessor delayedAuctionProcessor;

    /**
     * 경매 종료 처리 - 이전 실행 완료 후 1분 뒤 실행 (중복 실행 방지)
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)  // 이전 실행 후 1분 뒤
    public void processEndedAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // 종료된 경매 ID만 조회
        List<Long> endedItemIds = delayedItemRepository
            .findIdsByAuctionStatusInAndEndTimeBefore(
                List.of(AuctionStatus.BEFORE_BIDDING, AuctionStatus.IN_PROGRESS),
                now
            );

        if (endedItemIds.isEmpty()) {
            return;
        }

        for (Long itemId : endedItemIds) {
            try {
                delayedAuctionProcessor.processEndedAuction(itemId);
            } catch(Exception e) {
                log.error("경매 종료 처리 실패: itemId={}", itemId, e);
            }
        }
    }
}
