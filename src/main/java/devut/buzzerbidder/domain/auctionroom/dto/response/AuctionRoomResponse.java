package devut.buzzerbidder.domain.auctionroom.dto.response;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import java.util.List;

public record AuctionRoomResponse(

        Long remainingMs,

        List<AuctionRoomItemDto> items

) {
}
