package devut.buzzerbidder.domain.auctionroom.dto.response;

import java.time.LocalDateTime;

public record AuctionRoomSlotDto(
    LocalDateTime startAt,
    Integer roomCount
) {

}
