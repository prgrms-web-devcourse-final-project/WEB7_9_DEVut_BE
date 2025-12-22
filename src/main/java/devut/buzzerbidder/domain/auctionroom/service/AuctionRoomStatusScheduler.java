package devut.buzzerbidder.domain.auctionroom.service;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.AuctionStatus;
import devut.buzzerbidder.domain.auctionroom.repository.AuctionRoomRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionRoomStatusScheduler {

    private final AuctionRoomRepository auctionRoomRepository;
    private final AuctionRoomService auctionRoomService;

    //경매 시작 10분 전에 SCHEDULED 상태를 LIVE로 변경, 이전 실행 완료 후 1분 뒤 실행 (중복 실행 방지)
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)  // 이전 실행 후 1분 뒤
    public void updateScheduledToLive() {
        LocalDateTime now = LocalDateTime.now();
        
        // 경매 시작 10분 전인 SCHEDULED 상태의 경매방 ID만 조회 (N+1 방지)
        LocalDateTime targetTime = now.plusMinutes(10);
        
        // ID만 조회하여 N+1 문제 방지
        List<Long> scheduledRoomIds = auctionRoomRepository
            .findIdsByAuctionStatusAndLiveTimeLessThanEqual(
                AuctionStatus.SCHEDULED,
                targetTime
            );

        if (scheduledRoomIds.isEmpty()) {
            return;
        }

        log.info("경매방 상태 변경 시작: {}개의 경매방을 LIVE로 변경", scheduledRoomIds.size());

        // 각 경매방을 개별 트랜잭션으로 처리
        for (Long roomId : scheduledRoomIds) {
            try {
                auctionRoomService.processScheduledToLive(roomId);
            } catch (Exception e) {
                log.error("경매방 상태 변경 실패: roomId={}", roomId, e);
            }
        }
    }
}

