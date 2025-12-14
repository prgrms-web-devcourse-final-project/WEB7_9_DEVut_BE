package devut.buzzerbidder.domain.payment.service;

import devut.buzzerbidder.domain.payment.dto.PaymentHistoryItemDto;
import devut.buzzerbidder.domain.payment.dto.request.PaymentConfirmRequestDto;
import devut.buzzerbidder.domain.payment.dto.request.PaymentCreateRequestDto;
import devut.buzzerbidder.domain.payment.dto.request.PaymentFailRequestDto;
import devut.buzzerbidder.domain.payment.dto.request.PaymentHistoryRequestDto;
import devut.buzzerbidder.domain.payment.dto.response.PaymentConfirmResponseDto;
import devut.buzzerbidder.domain.payment.dto.response.PaymentCreateResponseDto;
import devut.buzzerbidder.domain.payment.dto.response.PaymentFailResponseDto;
import devut.buzzerbidder.domain.payment.dto.response.PaymentHistoryResponseDto;
import devut.buzzerbidder.domain.payment.entity.Payment;
import devut.buzzerbidder.domain.payment.entity.PaymentMethod;
import devut.buzzerbidder.domain.payment.entity.PaymentStatus;
import devut.buzzerbidder.domain.payment.repository.PaymentRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.TossPaymentsClient;
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.dto.response.TossConfirmResponseDto;
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

    @Transactional
    public PaymentCreateResponseDto create(Long userId, PaymentCreateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String orderId = genOrderId();
        Long amount = request.amount();

        Payment payment = Payment.create(user, orderId, "코인 충전", amount);
        paymentRepository.save(payment);

        return PaymentCreateResponseDto.from(payment);
    }

    public PaymentConfirmResponseDto confirm(PaymentConfirmRequestDto request) {
        String paymentKey = request.paymentKey();
        String orderId = request.orderId();
        Long amount = request.amount();

        Payment payment = paymentRepository.findByOrderId((orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        // 상태 검증
        if (!payment.isPending()) {
            throw new BusinessException(ErrorCode.NOT_PENDING_PAYMENT);
        }
        // 금액 검증
        if (!payment.getAmount().equals(amount)) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }

        TossConfirmResponseDto response = tossPaymentsClient.confirmPayment(request);
        // 응답상태(토스) 검증
        if (!response.status().equals("DONE")) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        payment.confirm(
                paymentKey,
                PaymentMethod.fromToss(response.method()),
                response.approvedAt());
        paymentRepository.save(payment);

        return PaymentConfirmResponseDto.from(payment);
    }

    public PaymentFailResponseDto fail(Long userId, PaymentFailRequestDto request) {
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
        // TODO: 최대 기간 검증

        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 10 : request.size();
        // TODO: 최대 사이즈 검증

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

//    public PaymentCancelResponseDto cancelPayment(Long userId, PaymentCancelRequestDto request) {
//        String cancelReason = request.cancelReason();
//        Payment payment = paymentRepository.findById(userId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
//
//        // 상태 검증
//        if (!payment.isSuccess()) {
//            throw new BusinessException((ErrorCode.NOT_SUCCESS_PAYMENT));
//        }
//
//        TossCancelResponseDto response = tossPaymentsClient.cancelPayment(payment.getPaymentKey(), request);
//        // 응답상태(토스) 검증
//        if (!response.status().equals("CANCELED")) {
//            throw new BusinessException(ErrorCode.PAYMENT_CANCELED_FAILED);
//        }
//
//        payment.cancel(
//                response.cancelAmount(),
//                cancelReason,
//                response.canceledAt()
//        );
//        paymentRepository.save(payment);
//
//        return PaymentCancelResponseDto.from(payment);
//    }

//    public PaymentWithdrawResponseDto withdrawPayment(Long userId, PaymentWithdrawRequestDto request) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
//
//        // TODO: 코인 잔액 검증 추가
//        // TODO: 코인 잠그기(상태변경)
//        // TODO: 출금 엔티티 생성 후 DB저장
//        // TODO: 응답 DTO 연결
//    }

    private String genOrderId() {
        return "ORDER_" + System.currentTimeMillis();
    }
}
