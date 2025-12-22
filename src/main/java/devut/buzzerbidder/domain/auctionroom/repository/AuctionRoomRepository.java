package devut.buzzerbidder.domain.auctionroom.repository;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.AuctionStatus;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.RoomStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionRoomRepository extends JpaRepository<AuctionRoom, Long> {
    Optional<AuctionRoom> findFirstByLiveTimeAndRoomStatus(
        LocalDateTime liveTime,
        RoomStatus roomStatus
    );

    @Query("SELECT ar.id FROM AuctionRoom ar WHERE ar.auctionStatus = :status AND ar.liveTime <= :targetTime")
    List<Long> findIdsByAuctionStatusAndLiveTimeLessThanEqual(
        @Param("status") AuctionStatus status,
        @Param("targetTime") LocalDateTime targetTime
    );
}
