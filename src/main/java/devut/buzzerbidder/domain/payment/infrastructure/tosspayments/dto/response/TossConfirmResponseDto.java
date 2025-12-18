package devut.buzzerbidder.domain.payment.infrastructure.tosspayments.dto.response;

import java.time.OffsetDateTime;

public record TossConfirmResponseDto(
        String paymentKey,
        String orderId,
        String status,
        String method,
        Long amount,
        OffsetDateTime approvedAt
) {
}
