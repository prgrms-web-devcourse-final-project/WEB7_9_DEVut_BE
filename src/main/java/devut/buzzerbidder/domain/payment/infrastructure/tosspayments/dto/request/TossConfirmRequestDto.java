package devut.buzzerbidder.domain.payment.infrastructure.tosspayments.dto.request;

public record TossConfirmRequestDto(
        String paymentKey,
        String orderId,
        Long amount
) {
}
