package devut.buzzerbidder.domain.delayedbid.event;

public record DelayedBidOutbidEvent(
    Long delayedItemId,
    String delayedItemName,
    Long previousBidderUserId,
    Long newBidderUserId,
    Long newBidAmount
) {

}
