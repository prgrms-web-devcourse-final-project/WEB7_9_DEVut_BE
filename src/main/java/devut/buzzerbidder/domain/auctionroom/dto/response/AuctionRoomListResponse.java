package devut.buzzerbidder.domain.auctionroom.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AuctionRoomListResponse(
    LocalDateTime startAt,
    List<AuctionRoomDto> rooms
) {

}
