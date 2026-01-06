package devut.buzzerbidder.domain.deal.event;

import devut.buzzerbidder.domain.deal.enums.AuctionType;

public record TransactionCompleteEvent(
    Long dealId,
    Long buyerId,
    Long sellerId,
    Long itemId,
    AuctionType auctionType,
    String itemName,
    Long finalPrice
) {

}
