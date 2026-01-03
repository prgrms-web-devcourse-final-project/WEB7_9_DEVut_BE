package devut.buzzerbidder.domain.wallet.dto.response;

import devut.buzzerbidder.domain.wallet.entity.WalletHistory;
import devut.buzzerbidder.domain.wallet.enums.WalletTransactionType;

import java.time.LocalDateTime;

public record HistoryResponseDto(
        LocalDateTime transactionDate,
        Long amount,
        WalletTransactionType transactionType,
        Long bizzBalanceAfter
) {
    public static HistoryResponseDto from(WalletHistory walletHistory) {
        Long amount;
        if (walletHistory.getType().isIncrease()) {
            amount = walletHistory.getAmount();
        } else {
            amount = -walletHistory.getAmount();
        }

        return new HistoryResponseDto(
                walletHistory.getCreateDate(),
                amount,
                walletHistory.getType(),
                walletHistory.getBizzBalanceAfter()
        );
    }
}
