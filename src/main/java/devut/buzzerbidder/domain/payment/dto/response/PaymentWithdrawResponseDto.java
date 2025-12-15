package devut.buzzerbidder.domain.payment.dto.response;

import java.time.LocalDateTime;

public record PaymentWithdrawResponseDto(
        Long withDrawId,
        //Long transactionId,
        Long userId,
        //Long balance,
        String status,
        String msg,
        LocalDateTime requestedAt
) {
}
