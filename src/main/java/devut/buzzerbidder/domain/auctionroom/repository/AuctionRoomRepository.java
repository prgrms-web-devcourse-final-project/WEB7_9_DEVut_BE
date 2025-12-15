package devut.buzzerbidder.domain.auctionroom.repository;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.RoomStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRoomRepository extends JpaRepository<AuctionRoom, Long> {
    Optional<AuctionRoom> findFirstByLiveTimeAndRoomStatus(
        LocalDateTime liveTime,
        RoomStatus roomStatus
    );
}
