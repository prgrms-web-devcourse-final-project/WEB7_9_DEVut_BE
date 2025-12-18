package devut.buzzerbidder.domain.deal.scheduler;

import devut.buzzerbidder.domain.deal.service.DelayedDealService;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.AuctionStatus;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DelayedAuctionScheduler {

    private final DelayedItemRepository delayedItemRepository;
    private final DelayedDealService delayedDealService;

    /**
     * 경매 종료 처리 - 1분마다 실행
     * IN_PROGRESS 상태이면서 endTime이 지난 경매를 종료하고 Deal 생성
     */
    @Scheduled(fixedRate = 60000)  // 1분마다
    @Transactional
    public void processEndedAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // 종료된 경매 조회
        List<DelayedItem> endedItems = delayedItemRepository
            .findByAuctionStatusAndEndTimeBefore(
                AuctionStatus.IN_PROGRESS,
                now
            );

        if (endedItems.isEmpty()) {
            return;
        }

        for (DelayedItem item : endedItems) {
            try {
                // 1. DelayedDeal 생성 (입찰이 있는 경우)
                delayedDealService.createDealFromAuction(item.getId());

                // 2. 상태를 IN_DEAL로 변경
                item.changeAuctionStatus(AuctionStatus.IN_DEAL);
            } catch(BusinessException e) {
                // 입찰이 없을시 FAILED로 변경
                if (e.getErrorCode() == ErrorCode.NO_BID_EXISTS) {
                    item.changeAuctionStatus(AuctionStatus.FAILED);
                } else {
                    log.error("경매 종료 처리 실패: itemId={}, error={}", item.getId(), e.getErrorCode(), e);
                }
            } catch (Exception e) {
                log.error("경매 종료 처리 중 예상치 못한 오류: itemId={}", item.getId(), e);
            }
        }
    }
}
