package devut.buzzerbidder.domain.auctionroom.dto.response;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.AuctionStatus;
import java.util.List;

public record AuctionRoomDto(
    Long roomId,
    Long roomIndex,
    AuctionStatus status,
    Long itemCount,
    List<LiveItemDto> items
) {


}
