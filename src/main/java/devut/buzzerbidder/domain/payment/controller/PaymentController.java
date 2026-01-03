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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment", description = "결제 API")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping()
    @Operation(summary = "결제 생성")
    public ApiResponse<PaymentCreateResponseDto> createPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentCreateRequestDto request
    ) {
        PaymentCreateResponseDto response = paymentService.create(userDetails.getId(), request);
        return ApiResponse.ok("결제가 생성되었습니다.", response);
    }

    @PostMapping("/confirm")
    @Operation(summary = "결제 승인")
    public ApiResponse<PaymentConfirmResponseDto> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentConfirmRequestDto request
    ) {
        PaymentConfirmResponseDto response = paymentService.confirm(userDetails.getId(), request);
        return ApiResponse.ok("결제가 승인되었습니다.", response);
    }

    @PostMapping("/fail")
    @Operation(summary = "결제 실패")
    public ApiResponse<PaymentFailResponseDto> failPayment(
            @Valid @RequestBody PaymentFailRequestDto request
    ) {
        PaymentFailResponseDto response = paymentService.fail(request);
        return ApiResponse.ok("결제가 실패했습니다.", response);
    }

    @GetMapping("/history")
    @Operation(summary = "결제 내역 조회")
    public ApiResponse<PaymentHistoryResponseDto> getPaymentHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        // 날짜 오프셋 형식으로 인코딩
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        OffsetDateTime start = startDate.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime end = endDate.plusDays(1).atStartOfDay(zoneId).minusNanos(1).toOffsetDateTime(); // 다음날 넘어가기 바로 직전의 종료일 조회

        PaymentHistoryRequestDto request = new PaymentHistoryRequestDto(start, end, status, page, size);
        PaymentHistoryResponseDto response = paymentService.getPaymentHistory(userDetails.getId(), request);
        return ApiResponse.ok("결제 내역 조회에 성공했습니다.", response);
    }


}



