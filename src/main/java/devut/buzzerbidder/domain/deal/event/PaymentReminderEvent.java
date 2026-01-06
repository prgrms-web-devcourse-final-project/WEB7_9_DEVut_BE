package devut.buzzerbidder.domain.deal.event;

public record PaymentReminderEvent(
    Long dealId,
    Long buyerId,
    Long itemId,
    String itemName,
    Long finalPrice,
    int remainingHours
) {

}
