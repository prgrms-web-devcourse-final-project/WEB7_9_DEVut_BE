package devut.buzzerbidder.domain.payment.service;

import devut.buzzerbidder.domain.payment.dto.PaymentHistoryItemDto;
import devut.buzzerbidder.domain.payment.dto.request.*;
import devut.buzzerbidder.domain.payment.dto.response.PaymentConfirmResponseDto;
import devut.buzzerbidder.domain.payment.dto.response.PaymentCreateResponseDto;
import devut.buzzerbidder.domain.payment.dto.response.PaymentFailResponseDto;
import devut.buzzerbidder.domain.payment.dto.response.PaymentHistoryResponseDto;
import devut.buzzerbidder.domain.payment.entity.Payment;
import devut.buzzerbidder.domain.payment.entity.PaymentMethod;
import devut.buzzerbidder.domain.payment.entity.PaymentStatus;
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.TossPaymentsClient;
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.dto.response.TossConfirmResponseDto;
import devut.buzzerbidder.domain.payment.repository.PaymentRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.wallet.repository.WalletRepository;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentTransactionService paymentTransactionService;

    @Transactional
    public PaymentCreateResponseDto create(Long userId, PaymentCreateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String orderId = genOrderId();  // 토스와 통신에 사용할 주문 ID 생성
        Long amount = request.amount();

        Payment payment = new Payment(user, orderId, "코인 충전", amount);
        paymentRepository.save(payment);

        return PaymentCreateResponseDto.from(payment);
    }


    public PaymentConfirmResponseDto confirm(Long userId, PaymentConfirmRequestDto request) {
        String paymentKey = request.paymentKey();
        String orderId = request.orderId();
        Long amount = request.amount();

        paymentTransactionService.markLocked(orderId, amount);

        TossConfirmResponseDto response = tossPaymentsClient.confirmPayment(request);

        // 토스의 응답상태 확인용
        if (!response.status().equals("DONE")) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        try {
            // 승인 요청 성공 시, 지갑 충전
            Payment payment = paymentTransactionService.confirmAndChargeWallet(userId, orderId, paymentKey, response, amount);
            return PaymentConfirmResponseDto.from(payment);

        } catch (Exception e) {
            PaymentCancelRequestDto cancelRequest = new PaymentCancelRequestDto("결제 승인 후 내부 처리 실패로 인한 취소");
            try {
                // DB 충전 중 오류 발생 시, 결제 취소 요청
                tossPaymentsClient.cancelPayment(paymentKey, cancelRequest);
                paymentTransactionService.markCanceled(orderId, "INTERNAL_FAIL", cancelRequest.cancelReason());
            } catch (Exception ex) {
                // 취소 실패 시, CANCEL_PENDING 상태 유지(스케줄러로 재시도 최대 3번)
                paymentTransactionService.markCancelPending(paymentKey, orderId, "CANCEL_FAIL", "결제 취소 실패", OffsetDateTime.now());
            }
                throw new BusinessException(ErrorCode.INTERNAL_PAYMENT_ERROR);
        }
    }

    public PaymentFailResponseDto fail(PaymentFailRequestDto request) {
        String orderId = request.orderId();
        String code = request.code();
        String msg = request.msg();

        Payment payment = paymentRepository.findByOrderId((orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.isPending()) {
            throw new BusinessException(ErrorCode.NOT_PENDING_PAYMENT);
        }

        payment.fail(code, msg);
        paymentRepository.save(payment);

        return PaymentFailResponseDto.from(payment);
    }

    public PaymentHistoryResponseDto getPaymentHistory(Long userId, PaymentHistoryRequestDto request) {
        OffsetDateTime startTime = request.startDate();
        OffsetDateTime endTime = request.endDate();
        String status = request.status();

        if (endTime.isBefore(startTime)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 10 : request.size();

        // 조회 성능 보호용(최대 크기 제한)
        if (page >= 15) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_ERROR);
        }
        if (size > 30) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "approvedAt"));
        Page<Payment> paymentPage;

        if (status == null || status.isBlank()) {
            paymentPage = paymentRepository.findByUserIdAndApprovedAtBetween(
                    userId,
                    startTime,
                    endTime,
                    pageable
            );
        } else {
            // 상태 존재 시, 해당 상태로 조회
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status);
            paymentPage = paymentRepository.findByUserIdAndApprovedAtBetweenAndStatus(
                    userId,
                    startTime,
                    endTime,
                    paymentStatus,
                    pageable
            );
        }

        List<PaymentHistoryItemDto> payments = paymentPage.getContent().stream()
                .map(PaymentHistoryItemDto::from)
                .toList();

        return PaymentHistoryResponseDto.from(payments, paymentPage);
    }

    private String genOrderId() {
        return "ORDER_" + System.currentTimeMillis();
    }
}
