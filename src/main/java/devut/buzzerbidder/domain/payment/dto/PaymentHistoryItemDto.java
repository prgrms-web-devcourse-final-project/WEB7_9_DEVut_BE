package devut.buzzerbidder.domain.payment.dto;

import devut.buzzerbidder.domain.payment.entity.Payment;

import java.time.OffsetDateTime;

public record PaymentHistoryItemDto(
        Long paymentId,
        OffsetDateTime PaymentDate,
        Long amount,
        String status,
        String description
) {
    public static PaymentHistoryItemDto from(Payment payment) {
        return new PaymentHistoryItemDto(
                payment.getId(),
                payment.getApprovedAt(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getOrderName()
        );
    }
}
