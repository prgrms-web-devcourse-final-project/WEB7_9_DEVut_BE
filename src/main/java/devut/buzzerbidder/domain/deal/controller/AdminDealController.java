package devut.buzzerbidder.domain.deal.controller;

import devut.buzzerbidder.domain.deal.dto.response.AdminDelayedDealResponseDto;
import devut.buzzerbidder.domain.deal.dto.response.AdminLiveDealResponseDto;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.service.AdminDealService;
import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/deals")
@Tag(name = "AdminDeal", description = "관리자 거래 API")
public class AdminDealController {

    private final AdminDealService adminDealService;

    @GetMapping("/delayed")
    @Operation(summary = "지연 경매 거래 내역 조회(관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDelayedDealResponseDto> getDelayedDeals(
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) DealStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "15") Integer size
    ) {
        AdminDelayedDealResponseDto response = adminDealService.getDelayedDeals(buyerId, status, page, size);
        return ApiResponse.ok("지연 경매 거래 내역 조회에 성공했습니다", response);
    }

    @GetMapping("/live")
    @Operation(summary = "라이브 경매 거래 내역 조회(관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminLiveDealResponseDto> getLiveDeals(
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) DealStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "15") Integer size
    ) {
        AdminLiveDealResponseDto response = adminDealService.getLiveDeals(buyerId, status, page, size);
        return ApiResponse.ok("라이브 경매 거래 내역 조회에 성공했습니다", response);
    }
}
