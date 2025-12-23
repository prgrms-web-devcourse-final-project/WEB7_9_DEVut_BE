package devut.buzzerbidder.domain.auctionroom.service;

import static reactor.netty.http.HttpConnectionLiveness.log;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.AuctionStatus;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.RoomStatus;
import devut.buzzerbidder.domain.auctionroom.repository.AuctionRoomRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionRoomService {

    private final AuctionRoomRepository auctionRoomRepository;

    public AuctionRoom assignRoom(LocalDateTime liveTime, long roomIndex) {

        List<AuctionRoom> rooms = auctionRoomRepository.findAllByLiveTime(liveTime);

        AuctionRoom targetRoom = null;
        boolean hasOpenRoom = false;

        // 순회하면서 OPEN 있는지 검사 + roomIndex로 방찾기
        for (AuctionRoom room : rooms) {

            if (room.getRoomStatus() == RoomStatus.OPEN) {
                hasOpenRoom = true;
            }

            if (room.getRoomIndex() == (roomIndex)) {
                targetRoom = room;
            }
        }

        // roomIndex 해당 경매방을 할당 가능한지 확인
        if (targetRoom != null) {

            if(targetRoom.getRoomStatus() == RoomStatus.OPEN) {
                return targetRoom;
            }

            if(targetRoom.getRoomStatus() == RoomStatus.FULL) {
                throw new BusinessException(ErrorCode.FULL_AUCTION_ROOM);
            }
        }

        // 해당 시간 경매방이 5개이상 + OPEN 상태인 방이 없음
        if (rooms.size() >= 5 && !hasOpenRoom) {
            throw new BusinessException(ErrorCode.AUCTION_ROOM_ASSIGN_UNAVAILABLE);
        }

        // 없으면 새 경매방 생성
        AuctionRoom newRoom = new AuctionRoom(liveTime, roomIndex);

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
