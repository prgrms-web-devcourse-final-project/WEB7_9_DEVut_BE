package devut.buzzerbidder.domain.deal.event;

public record DelayedAuctionEndedEvent(
    Long delayedItemId,
    String delayedItemName,
    Long sellerUsedId,
    boolean success,        // true = 낙찰, false = 유찰
    Long winnerUserId,      // 유찰이면 null
    Long finalPrice         // 유찰이면 null
) {

}
