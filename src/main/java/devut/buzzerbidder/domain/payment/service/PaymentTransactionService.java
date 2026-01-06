package devut.buzzerbidder.domain.payment.service;

import devut.buzzerbidder.domain.payment.entity.Payment;
import devut.buzzerbidder.domain.payment.entity.PaymentMethod;
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.dto.response.TossConfirmResponseDto;
import devut.buzzerbidder.domain.payment.repository.PaymentRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;

    @Transactional
    public void markLocked(String orderId, Long amount) {
        Payment payment = paymentRepository.findByOrderIdForLock((orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 이미 처리된 결제에 대해서 중복 승인 방지용
        if (!payment.isPending()) {
            throw new BusinessException(ErrorCode.NOT_PENDING_PAYMENT);
        }

        // 요청 금액과 결제 생성금액 동일한지 확인용
        if (!payment.getAmount().equals(amount)) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }

        payment.markLocked();
        paymentRepository.save(payment);
    }

    @Transactional
    public Payment confirmAndChargeWallet(Long userId, String orderId, String paymentKey, TossConfirmResponseDto response, Long amount) {
        Payment payment = paymentRepository.findByOrderIdForLock(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.isLocked()) {
            throw new BusinessException(ErrorCode.NOT_PENDING_PAYMENT);
        }

        payment.confirm(
                paymentKey,
                PaymentMethod.fromToss(response.method()),
                response.approvedAt()
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        walletService.chargeBizz(user, amount);
        return payment;
    }

    @Transactional
    public void markCanceled(String orderId, String code, String msg) {
        paymentRepository.findByOrderIdForLock(orderId)
                .ifPresent(payment -> payment.cancel(code, msg));
    }

    @Transactional
    public void markCancelFailed(String orderId, String code, String msg) {
        paymentRepository.findByOrderIdForLock(orderId)
                .ifPresent(payment -> payment.cancelFailed(code, msg));
    }

    @Transactional
    public void markFailed(String orderId, String code, String msg) {
        paymentRepository.findByOrderIdForLock(orderId)
                .ifPresent(payment -> payment.fail(code, msg));
    }
}
