package devut.buzzerbidder.domain.auctionroom.service;

import static reactor.netty.http.HttpConnectionLiveness.log;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.AuctionStatus;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.RoomStatus;
import devut.buzzerbidder.domain.auctionroom.repository.AuctionRoomRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionRoomService {

    private final AuctionRoomRepository auctionRoomRepository;

    public AuctionRoom assignRoom(LocalDateTime liveTime) {

        // 1. OPEN 상태인 방 검색
        Optional<AuctionRoom> roomOpt = auctionRoomRepository
            .findFirstByLiveTimeAndRoomStatus(liveTime, RoomStatus.OPEN);

        if (roomOpt.isPresent()) {
            return roomOpt.get();
        }

        // 2. 없으면 새 방 생성
        AuctionRoom newRoom = new AuctionRoom(liveTime);
        auctionRoomRepository.save(newRoom);
        return newRoom;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processScheduledToLive(Long roomId) {
        AuctionRoom room = auctionRoomRepository.findById(roomId)
            .orElse(null);

        if (room == null) {
            log.warn("경매방이 없음: roomId={}", roomId);
            return;
        }

        //상태 재확인
        if (room.getAuctionStatus() != AuctionStatus.SCHEDULED) {
            log.warn("이미 처리된 경매방: roomId={}, status={}", roomId, room.getAuctionStatus());
            return;
        }

        //시간 재확인
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime entryAllowedTime = room.getLiveTime().minusMinutes(10);
        if (now.isBefore(entryAllowedTime)) {
            log.warn("아직 입장 가능 시간이 아님: roomId={}, liveTime={}", roomId, room.getLiveTime());
            return;
        }

        //상태 변경
        try {
            room.startLive();
            auctionRoomRepository.save(room);
            log.info("경매방 상태 변경 완료: roomId={}, liveTime={}", roomId, room.getLiveTime());
        } catch (Exception e) {
            log.error("경매방 상태 변경 실패: roomId={}", roomId, e);
            throw e;
        }
    }

}
