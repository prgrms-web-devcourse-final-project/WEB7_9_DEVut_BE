package devut.buzzerbidder.domain.payment.dto.response;

import devut.buzzerbidder.domain.payment.entity.Withdraw;

import java.time.LocalDateTime;

public record WithdrawResponseDto(
        Long withDrawId,
        String status,
        String msg,
        LocalDateTime processedAt
) { public static WithdrawResponseDto from(Withdraw withdraw) {
    return new WithdrawResponseDto(
            withdraw.getId(),
            withdraw.getStatus().name(),
            "출금 요청이 정상적으로 접수되었습니다.",
            withdraw.getProcessedAt()
    );
    }
}