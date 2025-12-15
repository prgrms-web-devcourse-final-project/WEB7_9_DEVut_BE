package devut.buzzerbidder.domain.payment.dto.response;

import devut.buzzerbidder.domain.payment.entity.Payment;

public record PaymentCreateResponseDto(
        String orderId,
        String orderName,
        Long amount
) {
    public static PaymentCreateResponseDto from(Payment payment) {
        return new PaymentCreateResponseDto(
                payment.getOrderId(),
                payment.getOrderName(),
                payment.getAmount()
        );
    }
}
