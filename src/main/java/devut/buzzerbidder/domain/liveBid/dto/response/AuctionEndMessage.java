package devut.buzzerbidder.domain.liveBid.dto.response;

public record AuctionEndMessage(
    Long liveItemId,
    String result,
    Long winnerId,
    Integer finalPrice
) {
    public AuctionEndMessage(Long liveItemId, String result, Long winnerId, Integer finalPrice) {
        this.liveItemId = liveItemId;
        this.result = result;
        this.winnerId = winnerId;
        this.finalPrice = finalPrice;
    }


}
