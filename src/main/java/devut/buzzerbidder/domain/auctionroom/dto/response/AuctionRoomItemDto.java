package devut.buzzerbidder.domain.auctionroom.dto.response;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import java.util.List;

public record AuctionRoomItemDto(

    String name,
    List<String> imageUrls,
    Long price,
    AuctionStatus auctionStatus
) {

}
