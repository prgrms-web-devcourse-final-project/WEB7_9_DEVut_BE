package devut.buzzerbidder.domain.wallet.service;

import devut.buzzerbidder.domain.wallet.dto.AdminWithdrawalItemDto;
import devut.buzzerbidder.domain.wallet.dto.response.AdminWithdrawalResponseDto;
import devut.buzzerbidder.domain.wallet.entity.Withdrawal;
import devut.buzzerbidder.domain.wallet.enums.WithdrawalStatus;
import devut.buzzerbidder.domain.wallet.repository.WalletRepository;
import devut.buzzerbidder.domain.wallet.repository.WithdrawalRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminWithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;

    public AdminWithdrawalResponseDto getRequestedWithdrawal(Long userId, WithdrawalStatus status, Integer page, Integer size) {
        int lastPage = (page == null || page < 0) ? 0 : page;
        int lastSize = (size == null || size <= 0) ? 15 : size;

        Page<Withdrawal> withdrawalPage;
        Pageable pageable = PageRequest.of(lastPage, lastSize, Sort.by(Sort.Direction.DESC, "createDate"));


        if (userId == null && status == null) {
            withdrawalPage = withdrawalRepository.findAll(pageable); // 전체 유저의 전체 상태 조회
        } else if (userId == null) {
            withdrawalPage = withdrawalRepository.findByStatus(status, pageable); // 전체 유저의 해당 상태만 조회
        } else if (status == null) {
            withdrawalPage = withdrawalRepository.findByUserId(userId, pageable); // 해당 유저의 전체 상태 조회
        } else {
            withdrawalPage = withdrawalRepository.findByUserIdAndStatus(userId, status, pageable); // 해당 유저의 해당 상태만 조회
        }

        List<AdminWithdrawalItemDto> withdrawals = withdrawalPage.getContent().stream()
                .map(AdminWithdrawalItemDto::from)
                .toList();

        return AdminWithdrawalResponseDto.from(withdrawals, withdrawalPage);
    }

    @Transactional
    public void approve(Long withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findByIdForUpdate(withdrawalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WITHDRAWAL_NOT_FOUND));

        withdrawal.approve();
    }

    @Transactional
    public void reject(Long withdrawalId, String rejectReason) {
        Withdrawal withdrawal = withdrawalRepository.findByIdForUpdate(withdrawalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WITHDRAWAL_NOT_FOUND));


        walletService.refundBizz(withdrawal.getUser(), withdrawal.getAmount());
        withdrawal.reject(rejectReason);
    }
}
