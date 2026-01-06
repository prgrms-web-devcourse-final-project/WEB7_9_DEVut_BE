package devut.buzzerbidder.domain.liveBid.service;

import devut.buzzerbidder.domain.auctionroom.event.AuctionRoomStartedEvent;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionRoomStartedEventListener {

    private final LiveBidRedisService liveBidRedisService;

    /**
     * 경매방이 SCHEDULED -> LIVE로 바뀌는 시점(입장 10분 전)에 호출됨
     * 여기서 첫 아이템을 starting ZSET에 넣어줘야 starting 스케줄러가 실제 시작 시각에 startAuction을 호출할 수 있음.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomStarted(AuctionRoomStartedEvent event) {
        if (event.liveItemIds() == null || event.liveItemIds().isEmpty()) {
            log.info("AuctionRoomStartedEvent: 아이템 없음. roomId={}", event.roomId());
            return;
        }

        // 첫 아이템: id가 가장 작은 걸로 시작(정렬 보장 안 되면 이게 제일 안전)
        Long firstItemId = event.liveItemIds().stream().min(Long::compareTo).orElse(null);
        if (firstItemId == null) return;

        // 첫 시작 시각: 방의 liveTime(정각/30분)에 맞춰 시작
        long startAtMs = event.liveTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        // 혹시 서버가 늦게 떠서 이미 시간이 지난 경우: 바로(또는 0.5초 뒤) 시작시키기
        long nowMs = System.currentTimeMillis();
        if (startAtMs < nowMs) {
            startAtMs = nowMs + 500;
        }

        liveBidRedisService.upsertStartingZset(firstItemId, startAtMs);
        log.info("첫 아이템 startingZset 등록: roomId={}, itemId={}, startAtMs={}",
                event.roomId(), firstItemId, startAtMs);
    }
}
