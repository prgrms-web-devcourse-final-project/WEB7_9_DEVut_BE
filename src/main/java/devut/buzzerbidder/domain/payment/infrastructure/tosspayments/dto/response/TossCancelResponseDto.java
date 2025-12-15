package devut.buzzerbidder.domain.payment.infrastructure.tosspayments.dto.response;

import java.time.LocalDateTime;

public record TossCancelResponseDto(
        String paymentKey,
        String orderId,
        String status,
        Long totalAmount,
        Long cancelAmount,
        Long balanceAmount,
        LocalDateTime canceledAt
) { }
