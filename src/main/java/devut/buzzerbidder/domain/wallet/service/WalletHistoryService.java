package devut.buzzerbidder.domain.wallet.service;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.wallet.entity.WalletHistory;
import devut.buzzerbidder.domain.wallet.enums.WalletTransactionType;
import devut.buzzerbidder.domain.wallet.repository.WalletHistoryRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletHistoryService {

    private final WalletHistoryRepository walletHistoryRepository;

    // 로그 확인
    @Transactional(readOnly = true)
    public Page<WalletHistory> getWalletHistoriesPage(Long userId, int page) {
        if (page < 1) page = 1;
        Pageable pageable = PageRequest.of(page - 1, 10, Sort.by("createDate").descending());

        return walletHistoryRepository.findByUserId(userId, pageable);
    }

    // 지갑 히스토리 기록
    void recordWalletHistory(User user, Long amount, WalletTransactionType type,
                                     Long balanceBefore, Long balanceAfter) {
        WalletHistory walletHistory = WalletHistory.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .bizzBalanceBefore(balanceBefore)
                .bizzBalanceAfter(balanceAfter)
                .build();

        walletHistoryRepository.save(walletHistory);
    }

}
