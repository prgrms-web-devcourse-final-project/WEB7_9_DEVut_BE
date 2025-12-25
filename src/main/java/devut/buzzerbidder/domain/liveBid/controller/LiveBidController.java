package devut.buzzerbidder.domain.liveBid.controller;

import devut.buzzerbidder.domain.liveBid.dto.request.LiveBidRequest;
import devut.buzzerbidder.domain.liveBid.dto.response.LiveBidResponse;
import devut.buzzerbidder.domain.liveBid.service.LiveBidService;
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
@RequestMapping("/api/v1/auction/live")
@Tag(name = "LiveBid", description = "라이브 경매 입찰 API")
public class LiveBidController {

    private final LiveBidService liveBidService;

    @PostMapping("{id}/bid")
    @Operation(summary = "입찰 하기", description = "특정 라이브 경매 상품에 입찰을 시도합니다.")
    public ApiResponse<LiveBidResponse> bid(
            @Valid @RequestBody LiveBidRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LiveBidResponse response = liveBidService.bid(request, userDetails.getUser());

        // 입찰 성공/실패 여부는 response 내부에 담겨 있으므로 200 OK로 반환
        return ApiResponse.ok("입찰 시도 처리 완료", response);
    }
}