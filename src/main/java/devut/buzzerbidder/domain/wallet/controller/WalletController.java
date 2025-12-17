package devut.buzzerbidder.domain.wallet.controller;

import devut.buzzerbidder.domain.wallet.dto.request.WithdrawRequestDto;
import devut.buzzerbidder.domain.wallet.dto.response.WithdrawResponseDto;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.global.response.ApiResponse;
import devut.buzzerbidder.global.security.CustomUserDetails;
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
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/withdraw")
    public ApiResponse<WithdrawResponseDto> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WithdrawRequestDto request
    ) {
        WithdrawResponseDto response = walletService.withdraw(userDetails.getId(), request);
        return ApiResponse.ok("출금요청이 완료되었습니다.", response);
    }
}
