package devut.buzzerbidder.domain.delayedbid.event;

public record DelayedFirstBidEvent(
    Long delayedItemId,
    String delayedItemName,
    Long sellerUserId,
    Long firstBidderUserId,
    Long firstBidAmount
) {

}
