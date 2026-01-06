package devut.buzzerbidder.domain.deal.event;

public record PaymentCompleteEvent(
    Long dealId,
    Long buyerId,
    Long sellerId,
    Long itemId,
    String itemName,
    Long totalPrice,
    Long depositAmount,
    Long remainingAmount
) {

}
