package devut.buzzerbidder.domain.wallet.controller;

import devut.buzzerbidder.domain.wallet.dto.request.WithdrawalRequestDto;
import devut.buzzerbidder.domain.wallet.dto.response.WithdrawalResponseDto;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.global.response.ApiResponse;
import devut.buzzerbidder.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallet", description = "지갑 API")
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/withdrawal")
    @Operation(summary = "출금 요청")
    public ApiResponse<WithdrawalResponseDto> withdrawal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WithdrawalRequestDto request
    ) {
        WithdrawalResponseDto response = walletService.withdrawal(userDetails.getId(), request);
        return ApiResponse.ok("출금요청이 완료되었습니다.", response);
    }
}
