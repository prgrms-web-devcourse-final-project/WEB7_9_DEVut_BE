package devut.buzzerbidder.domain.deal.event;

public record PaymentTimeoutEvent(
    Long dealId,
    Long buyerId,
    Long sellerId,
    Long itemId,
    String itemName,
    Long finalPrice
) {

}
