package devut.buzzerbidder.domain.liveBid.dto;

public record LiveBidEvent(
        Long auctionRoomId,
        Long liveItemId,
        Long bidderId,
        Long sellerId,
        int bidPrice
) {}
