package devut.buzzerbidder.domain.liveBid.dto.response;

public record BidUpdateMessage(
        String type,
        Long liveItemId,
        int newPrice,
        Long bidderId
) {
    public BidUpdateMessage(String type, Long liveItemId, int newPrice, Long bidderId) {
        this.type = type;
        this.liveItemId = liveItemId;
        this.newPrice = newPrice;
        this.bidderId = bidderId;
    }
}
