package devut.buzzerbidder.domain.auctionroom.service;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.RoomStatus;
import devut.buzzerbidder.domain.auctionroom.repository.AuctionRoomRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

}
