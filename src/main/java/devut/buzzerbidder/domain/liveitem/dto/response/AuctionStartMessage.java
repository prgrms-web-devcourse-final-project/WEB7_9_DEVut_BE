package devut.buzzerbidder.domain.liveitem.dto.response;

public record AuctionStartMessage(
    String type,
    Long liveItemId,
    String itemName,
    Integer initPrice
) {
    public AuctionStartMessage(Long liveItemId, String itemName, Integer initPrice) {
        this("AUCTION_START", liveItemId, itemName, initPrice);
    }
}
