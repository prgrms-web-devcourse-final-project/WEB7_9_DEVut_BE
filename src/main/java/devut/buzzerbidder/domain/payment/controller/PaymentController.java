package devut.buzzerbidder.domain.payment.controller;

import devut.buzzerbidder.domain.payment.dto.request.PaymentConfirmRequestDto;
import devut.buzzerbidder.domain.payment.dto.request.PaymentCreateRequestDto;
import devut.buzzerbidder.domain.payment.dto.request.PaymentFailRequestDto;
import devut.buzzerbidder.domain.payment.dto.request.PaymentHistoryRequestDto;
import devut.buzzerbidder.domain.payment.dto.response.PaymentConfirmResponseDto;
import devut.buzzerbidder.domain.payment.dto.response.PaymentCreateResponseDto;
import devut.buzzerbidder.domain.payment.dto.response.PaymentFailResponseDto;
import devut.buzzerbidder.domain.payment.dto.response.PaymentHistoryResponseDto;
import devut.buzzerbidder.domain.payment.service.PaymentService;
import devut.buzzerbidder.global.response.ApiResponse;
import devut.buzzerbidder.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping()
    public ApiResponse<PaymentCreateResponseDto> createPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentCreateRequestDto request
    ) {
        PaymentCreateResponseDto response = paymentService.create(userDetails.getId(), request);

        return ApiResponse.ok("결제가 생성되었습니다.", response);
    }

    @PostMapping("/confirm")
    public ApiResponse<PaymentConfirmResponseDto> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentConfirmRequestDto request
    ) {
        PaymentConfirmResponseDto response = paymentService.confirm(request);

        return ApiResponse.ok("결제가 승인되었습니다.", response);
    }

    @PostMapping("/fail")
    public ApiResponse<PaymentFailResponseDto> failPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentFailRequestDto request
    ) {
        PaymentFailResponseDto response = paymentService.fail(userDetails.getId(), request);

        return ApiResponse.ok("결제가 실패했습니다.", response);
    }

    @GetMapping("/history")
    public ApiResponse<PaymentHistoryResponseDto> getPaymentHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentHistoryRequestDto request
    ) {
        PaymentHistoryResponseDto response = paymentService.getPaymentHistory(userDetails.getId(), request);

        return ApiResponse.ok("결제 내역 조회에 성공했습니다.", response);
    }

//    @PostMapping("/cancel")
//    public ApiResponse<PaymentCancelResponseDto> cancelPayment(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            @Valid @RequestBody PaymentCancelRequestDto request
//    ) {
//        PaymentCancelResponseDto response = paymentService.cancelPayment(userDetails.getId(), request);
//
//        return ApiResponse.ok("결제가 취소되었습니다.", response);
//    }

//    @PostMapping("/withdraw")
//    public ApiResponse<PaymentWithdrawResponseDto> withdrawPayment(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            @Valid @RequestBody PaymentWithdrawRequestDto request
//    ) {
//        PaymentWithdrawResponseDto response = paymentService.withdrawPayment(userDetails.getId(), request);
//
//        return ApiResponse.ok("결제환불 요청이 성공했습니다.", response);
//    }
}
