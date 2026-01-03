package devut.buzzerbidder.domain.auctionroom.entity;

import java.time.LocalDateTime;

public interface RoomCountByStartAtRow {
    LocalDateTime getStartAt();
    Long getRoomCount();
}
