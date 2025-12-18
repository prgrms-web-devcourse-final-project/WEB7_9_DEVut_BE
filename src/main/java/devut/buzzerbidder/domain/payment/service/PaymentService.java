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
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.TossPaymentsClient;
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.dto.response.TossConfirmResponseDto;
import devut.buzzerbidder.domain.payment.repository.PaymentRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.wallet.entity.Wallet;
import devut.buzzerbidder.domain.wallet.repository.WalletRepository;
import devut.buzzerbidder.domain.wallet.repository.WithdrawRepository;
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
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final WithdrawRepository withdrawRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;

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

        Payment payment = paymentRepository.findByOrderId((orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 이미 처리된 결제에 대해서 중복 승인 방지용
        if (!payment.isPending()) {
            throw new BusinessException(ErrorCode.NOT_PENDING_PAYMENT);
        }

        // 요청 금액과 결제 생성금액 동일한지 확인용
        if (!payment.getAmount().equals(amount)) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }

        TossConfirmResponseDto response = tossPaymentsClient.confirmPayment(request);

        // 토스의 응답상태 확인용
        if (!response.status().equals("DONE")) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        payment.confirm(
                paymentKey,
                PaymentMethod.fromToss(response.method()),
                response.approvedAt()
        );
        paymentRepository.save(payment);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        walletService.chargeBizz(user, amount);

        return PaymentConfirmResponseDto.from(payment);
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
