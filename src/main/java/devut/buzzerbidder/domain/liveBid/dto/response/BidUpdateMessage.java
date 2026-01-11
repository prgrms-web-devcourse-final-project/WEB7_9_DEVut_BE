package devut.buzzerbidder.domain.liveBid.dto.response;

public record BidUpdateMessage(
        String type,
        Long liveItemId,
        int newPrice,
        Long bidderId,
        String bidderNickname
) {
    public BidUpdateMessage(String type, Long liveItemId, int newPrice, Long bidderId, String bidderNickname) {
        this.type = type;
        this.liveItemId = liveItemId;
        this.newPrice = newPrice;
        this.bidderId = bidderId;
        this.bidderNickname = bidderNickname;
    }
}
