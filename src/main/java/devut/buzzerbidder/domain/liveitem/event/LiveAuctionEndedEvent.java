package devut.buzzerbidder.domain.liveitem.event;

public record LiveAuctionEndedEvent(
    Long liveItemId,
    String liveItemName,
    Long sellerUserId,
    boolean success,
    Long winnerUserId,
    Integer finalPrice
) {

}
