package devut.buzzerbidder.domain.liveitem.service;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemScheduler {

    private final LiveItemRepository liveItemRepository;
    private final LiveItemService liveItemService;

    /**
     * 60초마다 체크하여 경매 시작 및 종료를 처리
     */
    @Scheduled(fixedRate = 60000)
    public void processAuctions() {
        checkAndStartAuctions();
        checkAndEndAuctions();
    }

    // 경매 시작 처리
    private void checkAndStartAuctions() {
        LocalDateTime now = LocalDateTime.now();
        // 시작 시간이 지났고, 아직 대기 상태인 상품 조회
        List<Long> itemIds = liveItemRepository.findIdsToStart(now, LiveItem.AuctionStatus.BEFORE_BIDDING);

        for (Long id : itemIds) {
            try {
                liveItemService.startAuction(id);
            } catch (Exception e) {
                log.error("경매 시작 실패 - Item ID: {}, Error: {}", id, e.getMessage());
            }
        }
    }

    // 2. 경매 종료 처리
    private void checkAndEndAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // 진행 중(IN_PROGRESS)인 아이템 중 종료 시간이 지난 아이템 조회
        // 시작 시간으로부터 5분이 경과하면 종료로 간주
        List<LiveItem> itemsToEnd = liveItemRepository
                .findItemsToEnd(LiveItem.AuctionStatus.IN_PROGRESS, now.minusMinutes(5));

        for (LiveItem item : itemsToEnd) {
            try {
                liveItemService.endAuction(item.getId());
            } catch (Exception e) {
                log.error("경매 종료 처리 실패 - Item ID: {}, Error: {}", item.getId(), e.getMessage());
            }
        }
    }
}