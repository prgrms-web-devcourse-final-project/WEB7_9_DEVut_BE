package devut.buzzerbidder.domain.delayedbid.event;

public record DelayedBuyNowEvent(
    Long delayedItemId,
    String delayedItemName,
    Long buyUserId,
    Long sellerUserId,
    Long previousHighestBidderUserId,
    Long buyNowPrice
) {

}
