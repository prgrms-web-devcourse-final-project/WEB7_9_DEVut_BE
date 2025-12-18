package devut.buzzerbidder.domain.liveBid.dto.response;

public record BidUpdateMessage(
        Long liveItemId,
        int newPrice,
        Long bidderId
) {
    public BidUpdateMessage(Long liveItemId, int newPrice, Long bidderId) {
        this.liveItemId = liveItemId;
        this.newPrice = newPrice;
        this.bidderId = bidderId;
    }
}
