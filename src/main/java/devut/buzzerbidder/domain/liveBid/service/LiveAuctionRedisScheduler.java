package devut.buzzerbidder.domain.liveBid.service;

import devut.buzzerbidder.domain.liveitem.service.LiveItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveAuctionRedisScheduler {

    private final LiveBidRedisService liveBidRedisService;
    private final LiveItemService liveItemService;

    /**
     * ending ZSET 기반 종료 처리
     * - score(endTimeMs) <= nowMs 인 itemId를 pop 해서 endAuction 호출
     */
    @Scheduled(fixedDelay = 200) // 0.2초마다 (원하는대로 100~500ms)
    public void processEndings() {
        long nowMs = System.currentTimeMillis();
        List<Long> itemIds = liveBidRedisService.popDueEndingItems(nowMs, 50);

        for (Long itemId : itemIds) {
            try {
                liveItemService.endAuction(itemId);
            } catch (Exception e) {
                log.error("경매 종료 처리 실패 - Item ID: {}, Error: {}", itemId, e.getMessage());
                // 실패 시 재시도
                liveBidRedisService.upsertEndingZset(itemId, nowMs + 1000);
            }
        }
    }

    /**
     * starting ZSET 기반 시작 처리
     * - score(startTimeMs) <= nowMs 인 itemId를 pop 해서 startAuction 호출
     */
    @Scheduled(fixedDelay = 200)
    public void processStartings() {
        long nowMs = System.currentTimeMillis();
        List<Long> itemIds = liveBidRedisService.popDueStartingItems(nowMs, 50);

        for (Long itemId : itemIds) {
            try {
                liveItemService.startAuction(itemId);
            } catch (Exception e) {
                log.error("경매 시작 처리 실패 - Item ID: {}, Error: {}", itemId, e.getMessage());
                // 실패 시 재시도
                liveBidRedisService.upsertStartingZset(itemId, nowMs + 1000);
            }
        }
    }
}
