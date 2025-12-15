package devut.buzzerbidder.domain.liveBid.dto;

public record LiveBidRequest(
        Long liveItemId,
        Long bidderId,
        int bidPrice
) {
}
