package devut.buzzerbidder.domain.payment.dto.response;

import devut.buzzerbidder.domain.payment.entity.Payment;

import java.time.OffsetDateTime;

public record PaymentConfirmResponseDto(
        Long paymentId,
        String status,
        Long amount,
        OffsetDateTime approvedAt
) {
    public static PaymentConfirmResponseDto from(Payment payment) {
        return new PaymentConfirmResponseDto(
                payment.getId(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getApprovedAt()
        );
    }
}
