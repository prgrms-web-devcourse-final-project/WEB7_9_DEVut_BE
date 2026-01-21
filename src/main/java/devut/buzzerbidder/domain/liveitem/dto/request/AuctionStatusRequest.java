package devut.buzzerbidder.domain.liveitem.dto.request;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;

public record AuctionStatusRequest(
    AuctionStatus auctionStatus

) {
}
