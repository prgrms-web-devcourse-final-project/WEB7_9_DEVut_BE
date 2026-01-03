package devut.buzzerbidder.domain.wallet.service;

import devut.buzzerbidder.domain.wallet.dto.request.WithdrawalRequestDto;
import devut.buzzerbidder.domain.wallet.dto.response.HistoriesPageResponseDto;
import devut.buzzerbidder.domain.wallet.dto.response.HistoryResponseDto;
import devut.buzzerbidder.domain.wallet.dto.response.WithdrawalResponseDto;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.wallet.entity.Wallet;
import devut.buzzerbidder.domain.wallet.entity.WalletHistory;
import devut.buzzerbidder.domain.wallet.entity.Withdrawal;
import devut.buzzerbidder.domain.wallet.enums.WalletTransactionType;
import devut.buzzerbidder.domain.wallet.repository.WalletRepository;
import devut.buzzerbidder.domain.wallet.repository.WithdrawalRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WalletService {

    private final WalletRedisService walletRedisService;
    private final WalletHistoryService walletHistoryService;
    private final WalletRepository walletRepository;
    private final WithdrawalRepository withdrawRepository;
    private final UserRepository userRepository;

    // 잔액 조회
    @Transactional(readOnly = true)
    public Long getBizzBalance(User user) {
        // Redis 먼저 시도
        Long redisBizz = walletRedisService.getBizzBalance(user.getId());
        // Redis 조회 성공 시 그대로 반환
        if(redisBizz != null) return redisBizz;

        // Redis 조회 실패 시 DB에서 조회
        Wallet wallet = findByUserIdOrThrow(user.getId());
        return wallet.getBizz();
    }

    // 잔액이 충분한지 확인 (API 조회용)
    @Transactional(readOnly = true)
    public boolean hasEnoughBizz(User user, Long amount) {
        validateAmount(amount);

        return getBizzBalance(user) >= amount;
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
    public void bidBizz(User user, Long amount) {
        changeBizz(user, amount, WalletTransactionType.BID);
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

        Long fromId = fromUser.getId();
        Long toId = toUser.getId();

        boolean fromRedis = walletRedisService.isRedisActive(fromId);
        boolean toRedis   = walletRedisService.isRedisActive(toId);

        // 둘 다 Redis => Redis 처리
        if (fromRedis && toRedis) {
            WalletRedisService.RedisTransferResult r =
                    walletRedisService.transferBizzIfPresent(fromId, toId, amount, "TransferBizz", null);

            if  (!r.hit()) throw  new BusinessException(ErrorCode.TRANSFER_ERROR);
            return;
        }

        // 둘 다 DB => DB 처리
        if (!fromRedis && !toRedis) {
            transferDbFallback(fromUser, toUser, amount);
            return;
        }

        // from만 Redis, to는 DB
        if (fromRedis && !toRedis) {
            WalletRedisService.RedisBizzChangeResult out =
                    walletRedisService.changeBizzIfPresent(fromId, amount, false, "TransferBizz_OUT", null);

            if (!out.hit()) {
                transferDbFallback(fromUser, toUser, amount);
                return;
            }

            try {
                // 1) DB 입금 시도
                Wallet toWallet = findByUserIdWithLockOrThrow(toId);
                updateBizzAndRecordHistory(toWallet, toUser, amount, WalletTransactionType.RECEIVE_FROM_USER);

                // 2) "DB입금 성공 이후에만" 롤백 훅 등록
                runAfterRollback(() -> {
                    try {
                        walletRedisService.changeBizzIfPresent(fromId, amount, true, "TransferBizz_COMPENSATE_ROLLBACK", null);
                    } catch (Exception ex) {
                        log.error("rollback compensate failed. fromId={}, amount={}", fromId, amount, ex);
                    }
                });

            } catch (RuntimeException e) {
                // 3) DB 입금 실패면 즉시 환불
                try {
                    walletRedisService.changeBizzIfPresent(fromId, amount, true, "TransferBizz_COMPENSATE_FAIL", null);
                } catch (Exception ex) {
                    log.error("fail compensate failed. fromId={}, amount={}", fromId, amount, ex);
                }
                throw e;
            }
            return;
        }

        // to만 Redis, from은 DB
        if (!fromRedis && toRedis) {
            // DB 차감
            Wallet fromWallet = findByUserIdWithLockOrThrow(fromId);
            updateBizzAndRecordHistory(fromWallet, fromUser, amount, WalletTransactionType.PAY_TO_USER);

            // to는 Redis가 진실이므로 Redis에 입금
            WalletRedisService.RedisBizzChangeResult in =
                    walletRedisService.changeBizzIfPresent(toId, amount, true, "TransferBizz_IN", null);

            // Redis가 진실인데 MISS면 실패 처리
            if (!in.hit()) {
                throw new BusinessException(ErrorCode.TRANSFER_ERROR);
            }

            // 롤백되면 Redis 입금 되돌리기
            runAfterRollback(() -> {
                try {
                    walletRedisService.changeBizzIfPresent(toId, amount, false, "TransferBizz_COMPENSATE_ROLLBACK", null);
                } catch (Exception e) {
                    log.error("rollback compensate failed. toId={}, amount={}", toId, amount, e);
                }
            });
        }
    }


    // 보유 잔액 검증 후, 출금 요청
    @Transactional
    public WithdrawalResponseDto withdrawal(Long userId, WithdrawalRequestDto request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Long amount = request.amount();
        validateAmount(amount);

        Wallet wallet = findByUserIdWithLockOrThrow(userId);
        if (wallet.getBizz() < amount) {
            throw new BusinessException(ErrorCode.BIZZ_INSUFFICIENT_BALANCE);
        }

        updateBizzAndRecordHistory(wallet, user, amount, WalletTransactionType.WITHDRAW);

        Withdrawal withdrawal = new Withdrawal(user,
                amount,
                request.bankName(),
                request.accountNumber(),
                request.accountHolder()
        );
        withdrawRepository.save(withdrawal);

        return WithdrawalResponseDto.from(withdrawal);
    }

    public HistoriesPageResponseDto getHistories(Long userId, int page) {
        Page<WalletHistory> walletHistoriesPage = walletHistoryService.getWalletHistoriesPage(userId, page);

        List<HistoryResponseDto> walletHistories = walletHistoriesPage.getContent()
                .stream()
                .map(HistoryResponseDto::from)
                .toList();

        return HistoriesPageResponseDto.from(walletHistoriesPage, walletHistories);
    }

    /* ==================== 이 밑으로는 헬퍼 메서드 ==================== */

    private void transferDbFallback(User fromUser, User toUser, Long amount) {
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

        // 송금 실행
        updateBizzAndRecordHistory(fromWallet, fromUser, amount, WalletTransactionType.PAY_TO_USER);
        updateBizzAndRecordHistory(toWallet, toUser, amount, WalletTransactionType.RECEIVE_FROM_USER);
    }

    /** 트랜잭션이 "롤백"으로 끝나면 실행할 보상 로직 */
    private void runAfterRollback(Runnable compensate) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    compensate.run();
                }
            }
        });
    }

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

    // 금액 검증
    private void validateAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_WALLET_AMOUNT);
        }
    }

    // 단일 지갑 잔액 변경 공통 처리
    private void changeBizz(User user, Long amount, WalletTransactionType type) {
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        validateAmount(amount);

        // Redis 먼저 시도
        WalletRedisService.RedisBizzChangeResult redisResult =
                walletRedisService.changeBizzIfPresent(
                        user.getId(),
                        amount,
                        type.isIncrease(),
                        type.name(),    // reason
                        null    //traceId
                );
        // Redis에서 처리됐으면 그대로 함수 종료
        if (redisResult.hit()) return;

        // Redis에 없으면 DB 처리
        Wallet wallet = findByUserIdWithLockOrThrow(user.getId());
        updateBizzAndRecordHistory(wallet, user, amount, type);
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