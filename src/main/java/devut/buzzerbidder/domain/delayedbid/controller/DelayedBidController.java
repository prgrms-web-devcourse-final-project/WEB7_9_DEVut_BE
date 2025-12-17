package devut.buzzerbidder.domain.delayedbid.controller;

import devut.buzzerbidder.domain.delayedbid.dto.DelayedBidListResponse;
import devut.buzzerbidder.domain.delayedbid.dto.DelayedBidRequest;
import devut.buzzerbidder.domain.delayedbid.dto.DelayedBidResponse;
import devut.buzzerbidder.domain.delayedbid.service.DelayedBidService;
import devut.buzzerbidder.domain.liveitem.dto.request.PagingRequest;
import devut.buzzerbidder.global.response.ApiResponse;
import devut.buzzerbidder.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auction/delayed")
@Tag(name = "DelayedBid", description = "지연 경매 입찰 API")
public class DelayedBidController {

    private final DelayedBidService delayedBidService;

    @PostMapping("/{id}/bid")
    @Operation(summary = "지연 경매 입찰하기")
    public ApiResponse<DelayedBidResponse> placeBid(
        @PathVariable Long id,
        @RequestBody DelayedBidRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DelayedBidResponse response = delayedBidService.placeBid(
            id,
            request,
            userDetails.getUser()
        );

        return ApiResponse.ok("입찰 완료", response);
    }

    @GetMapping("/{id}/bids")
    @Operation(summary = "지연 경매 입찰 내역 조회")
    public ApiResponse<DelayedBidListResponse> getBidHistory(
        @PathVariable Long id,
        PagingRequest paging
    ) {
        DelayedBidListResponse response = delayedBidService.getBidHistory(
            id,
            paging.toPageable()
        );

        return ApiResponse.ok("입찰 내역 조회", response);
    }

    @GetMapping("/{id}/highest-bid")
    @Operation(summary = "최고가 입찰 조회")
    public ApiResponse<DelayedBidResponse> getHighestBid(
        @PathVariable Long id
    ) {
        DelayedBidResponse response = delayedBidService.getHighestBid(id);

        return ApiResponse.ok("최고가 입찰 조회", response);
    }

    @GetMapping("/my-bids")
    @Operation(summary = "내 입찰 내역 조회")
    public ApiResponse<DelayedBidListResponse> getMyBids(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        PagingRequest paging
    ) {
        DelayedBidListResponse response = delayedBidService.getMyBids(
            userDetails.getUser(),
            paging.toPageable()
        );

        return ApiResponse.ok("내 입찰 내역 조회", response);
    }

}
