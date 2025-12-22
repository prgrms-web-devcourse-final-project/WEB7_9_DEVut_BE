package devut.buzzerbidder.domain.wallet.dto;

import devut.buzzerbidder.domain.wallet.entity.Withdrawal;

import java.time.LocalDateTime;

public record AdminWithdrawalItemDto(
        Long withdrawalId,
        Long userId,
        Long amount,
        String bankName,
        String accountNumber,
        String accountHolder,
        String status,
        String msg,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
    public static AdminWithdrawalItemDto from(Withdrawal withdrawal)
    {
        return new AdminWithdrawalItemDto(
                withdrawal.getId(),
                withdrawal.getUser().getId(),
                withdrawal.getAmount(),
                withdrawal.getBankName(),
                withdrawal.getAccountNumber(),
                withdrawal.getAccountHolder(),
                withdrawal.getStatus().name(),
                withdrawal.getMsg(),
                withdrawal.getProcessedAt(),
                withdrawal.getCreateDate()
        );
    }
}
