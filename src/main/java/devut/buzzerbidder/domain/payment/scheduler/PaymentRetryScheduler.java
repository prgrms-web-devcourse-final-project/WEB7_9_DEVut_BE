package devut.buzzerbidder.domain.payment.scheduler;

import devut.buzzerbidder.domain.payment.dto.request.PaymentCancelRequestDto;
import devut.buzzerbidder.domain.payment.entity.Payment;
import devut.buzzerbidder.domain.payment.entity.PaymentStatus;
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.TossPaymentsClient;
import devut.buzzerbidder.domain.payment.repository.PaymentRepository;
import devut.buzzerbidder.domain.payment.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentRetryScheduler {

    private final PaymentRepository paymentRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentTransactionService paymentTransactionService;

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void retryCancel() {
        List<Payment> payments = paymentRepository.findDueCancelPending(OffsetDateTime.now(), PageRequest.of(0, 50));
        for (Payment payment : payments) {
            retry(payment.getOrderId());
        }
    }

    // 재시도 로직
    private void retry(String orderId) {
        paymentRepository.findByOrderIdForLock(orderId)
                .ifPresent(payment -> {
                    if (payment.getStatus() != PaymentStatus.CANCEL_PENDING) return;
                    if (payment.getNextCancelRetryAt() != null && payment.getNextCancelRetryAt().isAfter(OffsetDateTime.now())) return;
                    if (payment.getCancelRetryCount() >= 4) {
                        payment.failAfterThreeRetry("CANCEL_FAIL", "결제 취소 재시도 실패");
                        return;
                    }

                    try {
                       PaymentCancelRequestDto request = new PaymentCancelRequestDto("결제 승인 후 내부 처리 실패로 인한 취소");
                        tossPaymentsClient.cancelPayment(payment.getPaymentKey(), request);
                       payment.canceled("INTERNAL_FAIL", request.cancelReason());
                    } catch (Exception ex) {
                        int nextRetryCount = payment.getCancelRetryCount() + 1;

                        // 취소 최대 횟수 3번 이상은 CANCEL_FAILED 상태 변경
                        if (nextRetryCount >= 4){
                           payment.failAfterThreeRetry("CANCEL_FAIL", "결제 취소 재시도 실패");
                           return;
                        }

                        OffsetDateTime nextRetryAt = OffsetDateTime.now().plusSeconds(delaySeconds(nextRetryCount));

                        payment.increaseCancelRetryCount(nextRetryAt);
                        payment.cancelPending("CANCEL_FAIL", "결제 취소 실패", nextRetryAt);  // 취소 요청 실패 시, PENDING 상태 유지
                    }
                });
    }

    private long delaySeconds(int nextRetryCount) {
        return switch (nextRetryCount) {
            case 1 -> 30;
            case 2 -> 120;
            case 3 -> 300;
            default -> 120;
        };
    }
}
