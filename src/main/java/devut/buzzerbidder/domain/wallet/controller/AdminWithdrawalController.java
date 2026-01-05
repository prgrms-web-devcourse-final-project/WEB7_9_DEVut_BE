package devut.buzzerbidder.domain.wallet.controller;

import devut.buzzerbidder.domain.wallet.dto.request.WithdrawalRejectRequestDto;
import devut.buzzerbidder.domain.wallet.dto.response.AdminWithdrawalResponseDto;
import devut.buzzerbidder.domain.wallet.enums.WithdrawalStatus;
import devut.buzzerbidder.domain.wallet.service.AdminWithdrawalService;
import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/withdrawals")
@Tag(name = "AdminWallet", description = "관리자 지갑 API")
public class AdminWithdrawalController {

    private final AdminWithdrawalService adminWithdrawalService;

    @GetMapping()
    @Operation(summary = "출금 요청 내역 조회(관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminWithdrawalResponseDto> getRequestedWithdrawals(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) WithdrawalStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "15") Integer size
            ) {
        AdminWithdrawalResponseDto response = adminWithdrawalService.getRequestedWithdrawals(userId, status, page, size);
        return ApiResponse.ok("출금 요청 내역 조회에 성공했습니다", response);
    }

    @PostMapping("/{withdrawalId}/approve")
    @Operation(summary = "출금 요청 승인(관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> approveWithdrawal(
            @PathVariable Long withdrawalId
    ) {
        adminWithdrawalService.approve(withdrawalId);
        return ApiResponse.ok("출금 요청이 승인되었습니다.");
    }

    @PostMapping("/{withdrawalId}/reject")
    @Operation(summary = "출금 요청 거절(관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> rejectWithdrawal(
            @PathVariable Long withdrawalId,
            @RequestBody WithdrawalRejectRequestDto request
    ) {
        adminWithdrawalService.reject(withdrawalId, request.rejectReason());
        return ApiResponse.ok("출금 요청이 거절되었습니다.");
    }
}
