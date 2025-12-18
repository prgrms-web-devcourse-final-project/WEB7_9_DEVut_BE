package devut.buzzerbidder.domain.wallet.dto.response;

import devut.buzzerbidder.domain.wallet.entity.Withdrawal;

import java.time.LocalDateTime;

public record WithdrawalResponseDto(
        Long withDrawId,
        String status,
        String msg,
        LocalDateTime processedAt
) { public static WithdrawalResponseDto from(Withdrawal withdrawal) {
    return new WithdrawalResponseDto(
            withdrawal.getId(),
            withdrawal.getStatus().name(),
            "출금 요청이 정상적으로 접수되었습니다.",
            withdrawal.getProcessedAt()
    );
    }
}