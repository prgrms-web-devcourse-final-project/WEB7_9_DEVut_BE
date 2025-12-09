package devut.buzzerbidder.domain.deal.controller;

import devut.buzzerbidder.domain.deal.dto.TrackingNumberRequest;
import devut.buzzerbidder.domain.deal.enums.AuctionType;
import devut.buzzerbidder.domain.deal.service.LiveDealService;
import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/deals")
@RequiredArgsConstructor
@Tag(name = "Deals", description = "거래 API(공통)")
public class DealController {

    private final LiveDealService liveDealService;

    @PatchMapping("/{type}/{dealId}/delivery-info")
    @Operation(summary = "배송 정보 입력")
    public ApiResponse<Void> patchDeliveryInfo(
            @PathVariable String type,
            @PathVariable Long dealId,
            @RequestBody TrackingNumberRequest request
    ) {
        AuctionType auctionType = AuctionType.fromString(type);
        if(auctionType.equals(AuctionType.LIVE))
            liveDealService.patchDeliveryInfo(dealId, request.carrierCode(), request.trackingNumber());
//        TODO: else if(지연경매 코드)

        return ApiResponse.ok("운송장 번호가 입력되었습니다.", null);
    }
}
