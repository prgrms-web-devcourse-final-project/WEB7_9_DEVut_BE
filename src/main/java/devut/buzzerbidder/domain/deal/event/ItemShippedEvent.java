package devut.buzzerbidder.domain.deal.event;

import devut.buzzerbidder.domain.deal.enums.AuctionType;

public record ItemShippedEvent(
    Long dealId,
    Long buyerId,
    Long sellerId,
    Long itemId,
    AuctionType auctionType,
    String itemName,
    String carrierName,
    String trackingNumber
) {

}
