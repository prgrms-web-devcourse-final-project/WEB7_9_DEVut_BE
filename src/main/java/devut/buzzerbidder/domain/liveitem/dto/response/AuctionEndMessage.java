package devut.buzzerbidder.domain.liveitem.dto.response;

public record AuctionEndMessage(
    String type,
    Long liveItemId,
    String liveItemName,
    String result,
    Long winnerId,
    Integer finalPrice
) {
    public AuctionEndMessage(Long liveItemId, String liveItemName, String result, Long winnerId, Integer finalPrice) {
        this("AUCTION_END", liveItemId, liveItemName, result, winnerId, finalPrice);
    }
}
