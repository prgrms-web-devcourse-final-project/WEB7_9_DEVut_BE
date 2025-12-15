package devut.buzzerbidder.domain.payment.dto.response;

import devut.buzzerbidder.domain.payment.entity.Payment;

public record PaymentFailResponseDto(
        String status,
        String code,
        String msg
) {
    public static PaymentFailResponseDto from(Payment payment) {
        return new PaymentFailResponseDto(
                payment.getStatus().name(),
                payment.getFailCode(),
                payment.getFailReason()
        );
    }
}
