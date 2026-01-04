package devut.buzzerbidder.domain.auction.controller;

import devut.buzzerbidder.domain.auction.dto.request.AuctionSearchRequest;
import devut.buzzerbidder.domain.auction.dto.response.AuctionListResponse;
import devut.buzzerbidder.domain.auction.service.AuctionService;
import devut.buzzerbidder.global.response.ApiResponse;
import devut.buzzerbidder.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "통합 경매 API", description = "라이브 경매 + 지연 경매 통합 조회/검색")
@RestController
@RequestMapping("/api/v1/auction/search")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @Operation(
        summary = "통합 경매 검색",
        description = "라이브 경매와 지연 경매를 통합하여 검색, 필터링, 정렬, 페이징 지원"
    )
    @GetMapping
    public ApiResponse<AuctionListResponse> searchAuctions(
        AuctionSearchRequest searchRequest,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Long userId = userDetails != null ? userDetails.getUser().getId() : null;

        AuctionListResponse response = auctionService.searchAuctions(searchRequest, pageable, userId);
        return ApiResponse.ok("통합 경매 검색", response);
    }

}
