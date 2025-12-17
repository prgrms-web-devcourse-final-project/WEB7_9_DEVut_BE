package devut.buzzerbidder.domain.wallet.service;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.wallet.entity.Wallet;
import devut.buzzerbidder.domain.wallet.enums.WalletTransactionType;
import devut.buzzerbidder.domain.wallet.repository.WalletRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletHistoryService walletHistoryService;

    // 잔액 조회
    @Transactional(readOnly = true)
    public Long getBizzBalance(User user) {
        Wallet wallet = findByUserIdOrThrow(user.getId());
        return wallet.getBizz();
    }

    // 잔액이 충분한지 확인 (API 조회용)
    @Transactional(readOnly = true)
    public boolean hasEnoughBizz(User user, Long amount) {
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        validateAmount(amount);

        Wallet wallet = findByUserIdOrThrow(user.getId());
        return wallet.getBizz() >= amount;
    }

    // 지갑 존재 확인
    @Transactional(readOnly = true)
    public boolean hasWallet(Long userId) {
        return walletRepository.existsByUserId(userId);
    }

    // 지갑 생성
    public void createWallet(User user) {
        if (walletRepository.existsByUserId(user.getId())) {
            throw new BusinessException(ErrorCode.WALLET_ALREADY_EXISTS);
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .bizz(0L)
                .build();
        walletRepository.save(wallet);
    }

    // 충전
    public void chargeBizz(User user, Long amount) {
        changeBizz(user, amount, WalletTransactionType.CHARGE);
    }

    // 환불(거래 취소 시)
    public void refundBizz(User user, Long amount) {
        changeBizz(user, amount, WalletTransactionType.REFUND);
    }

    // 관리자 지급
    public void grantBizz(User user, Long amount) {
        changeBizz(user, amount, WalletTransactionType.ADMIN_GRANT);
    }

    // 출금
    public void withdrawBizz(User user, Long amount) {
        changeBizz(user, amount, WalletTransactionType.WITHDRAW);
    }

    // 관리자 차감
    public void deductBizz(User user, Long amount) {
        changeBizz(user, amount, WalletTransactionType.ADMIN_DEDUCT);
    }

    // 입찰 시 코인 차감
    public void lockBizzForBid(User user, Long amount) {
        changeBizz(user, amount, WalletTransactionType.BID_LOCK);
    }

    // 입찰 실패 시 코인 환불
    public void refundBidBizz(User user, Long amount) {
        changeBizz(user, amount, WalletTransactionType.BID_REFUND);
    }

    // A유저 -> B유저 송금
    public void transferBizz(User fromUser, User toUser, Long amount) {
        if (fromUser == null || toUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        validateAmount(amount);

        if (fromUser.getId().equals(toUser.getId())) {
            throw new BusinessException(ErrorCode.INVALID_TRANSFER);
        }

        // ID 순서대로 락 획득하여 데드락 방지
        Long smallerId = Math.min(fromUser.getId(), toUser.getId());
        Long largerId = Math.max(fromUser.getId(), toUser.getId());

        Wallet firstWallet = findByUserIdWithLockOrThrow(smallerId);
        Wallet secondWallet = findByUserIdWithLockOrThrow(largerId);

        // 송금자/수신자 구분
        Wallet fromWallet = firstWallet.getUser().getId().equals(fromUser.getId())
                ? firstWallet : secondWallet;
        Wallet toWallet = firstWallet.getUser().getId().equals(toUser.getId())
                ? firstWallet : secondWallet;

        // 잔액 확인
        if (fromWallet.getBizz() < amount) {
            throw new BusinessException(ErrorCode.BIZZ_INSUFFICIENT_BALANCE);
        }

        // 송금 실행
        updateBizzAndRecordHistory(fromWallet, fromUser, amount, WalletTransactionType.PAY_TO_USER);
        updateBizzAndRecordHistory(toWallet, toUser, amount, WalletTransactionType.RECEIVE_FROM_USER);
    }

    /* ==================== 이 밑으로는 헬퍼 메서드 ==================== */

    // 지갑 조회
    private Wallet findByUserIdOrThrow(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
    }

    // 지갑 조회(비관적 락 적용)
    private Wallet findByUserIdWithLockOrThrow(Long userId) {
        return walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
    }

    // 단일 지갑 잔액 변경 공통 처리 (비관적 락 적용)
    private void changeBizz(User user, Long amount, WalletTransactionType type) {
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        validateAmount(amount);

        Wallet wallet = findByUserIdWithLockOrThrow(user.getId());
        updateBizzAndRecordHistory(wallet, user, amount, type);
    }

    // 금액 검증
    private void validateAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_WALLET_AMOUNT);
        }
    }

    // 잔액 업데이트 + 히스토리 기록
    private void updateBizzAndRecordHistory(Wallet wallet, User user, Long amount, WalletTransactionType type) {
        Long balanceBefore = wallet.getBizz();

        if (type.isIncrease()) {
            wallet.increaseBizz(amount);
        } else {
            wallet.decreaseBizz(amount);
        }

        Long balanceAfter = wallet.getBizz();
        walletHistoryService.recordWalletHistory(user, amount, type, balanceBefore, balanceAfter);
    }

}