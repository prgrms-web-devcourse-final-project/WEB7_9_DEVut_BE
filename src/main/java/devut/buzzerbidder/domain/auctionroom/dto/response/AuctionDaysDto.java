package devut.buzzerbidder.domain.auctionroom.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AuctionDaysDto(
    LocalDate date,
    List<AuctionRoomSlotDto> slots
) {

}
