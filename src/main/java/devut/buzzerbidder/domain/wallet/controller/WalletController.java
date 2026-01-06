package devut.buzzerbidder.domain.wallet.controller;

import devut.buzzerbidder.domain.wallet.dto.request.WithdrawalRequestDto;
import devut.buzzerbidder.domain.wallet.dto.response.BizzResponseDto;
import devut.buzzerbidder.domain.wallet.dto.response.HistoriesPageResponseDto;
import devut.buzzerbidder.domain.wallet.dto.response.WithdrawalResponseDto;
import devut.buzzerbidder.domain.wallet.service.WalletRedisService;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.global.response.ApiResponse;
import devut.buzzerbidder.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallet")
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

    @GetMapping("/histories")
    @Operation(summary = "지갑 히스토리 목록 조회")
    public ApiResponse<HistoriesPageResponseDto> getHistories(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page
    ) {
        HistoriesPageResponseDto response = walletService.getHistories(userDetails.getId(), page);
        return ApiResponse.ok("지갑 히스토리 목록 조회 성공", response);
    }

    @GetMapping("/bizz")
    @Operation(summary = "보유 bizz 조회")
    public ApiResponse<BizzResponseDto> getBizz(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long bizz = walletService.getBizzBalance(userDetails.getUser());

        return ApiResponse.ok("Bizz 조회 성공",  BizzResponseDto.from(bizz));
    }

    // 임시
    private final WalletRedisService walletRedisService;
    @GetMapping("/isRedis")
    public ApiResponse<Boolean> isRedis(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long redisBizz = walletRedisService.getBizzBalance(userDetails.getId());
        boolean redis = redisBizz != null;

        return ApiResponse.ok(redis);
    }

}
