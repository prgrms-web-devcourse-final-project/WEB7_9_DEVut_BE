package devut.buzzerbidder.domain.liveitem.controller;

import devut.buzzerbidder.domain.liveitem.dto.request.AuctionStatusRequest;
import devut.buzzerbidder.domain.liveitem.dto.request.LiveItemCreateRequest;
import devut.buzzerbidder.domain.liveitem.dto.request.LiveItemModifyRequest;
import devut.buzzerbidder.domain.liveitem.dto.request.LiveItemSearchRequest;
import devut.buzzerbidder.domain.liveitem.dto.request.PagingRequest;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemDetailResponse;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemListResponse;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemResponse;
import devut.buzzerbidder.domain.liveitem.service.LiveItemService;
import devut.buzzerbidder.global.response.ApiResponse;
import devut.buzzerbidder.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auction/live")
@Tag(name = "LiveItem", description = "경매품 API")
public class LiveItemController {

    private final LiveItemService liveItemService;

    @PostMapping
    @Operation(summary = "경매품 생성")
    public ApiResponse<LiveItemResponse> createLiveItem(
        @RequestBody LiveItemCreateRequest reqBody,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ){

        LiveItemResponse response = liveItemService.writeLiveItem(reqBody, userDetails.getUser());

        return ApiResponse.ok("경매품 생성",response);

    }

    @PutMapping("/{id}")
    @Operation(summary = "경매품 정보 수정")
    public ApiResponse<LiveItemResponse> modifyLiveItem(
        @PathVariable Long id,
        @RequestBody LiveItemModifyRequest reqBody,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        LiveItemResponse response =liveItemService.modifyLiveItem(id, reqBody, userDetails.getUser());

        return ApiResponse.ok("%d번 경매품 수정".formatted(id), response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "경매품 삭제")
    public ApiResponse<Void> deleteLiveItem(
        @PathVariable Long id,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        liveItemService.deleteLiveItem(id, userDetails.getUser());

        return ApiResponse.ok("%d번 경매품 삭제".formatted(id));

    }

    @GetMapping
    @Operation(summary = "경매품 다건 조회")
    public ApiResponse<LiveItemListResponse> getLiveItems(
        LiveItemSearchRequest reqBody,
        PagingRequest paging
    ) {

        LiveItemListResponse response =
            liveItemService.getLiveItems(reqBody, paging.toPageable());

        return ApiResponse.ok("경매품 다건 조회", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "경매품 단건 조회")
    public ApiResponse<LiveItemDetailResponse> getLiveItem(
        @PathVariable Long id,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        LiveItemDetailResponse response = liveItemService.getLiveItem(id);


        return ApiResponse.ok("%d번 경매품 단건 조회".formatted(id), response);
    }

    @GetMapping("/hot")
    @Operation(summary = "인기 경매품 조회")
    public ApiResponse<LiveItemListResponse> getHotLiveItems(
        @RequestParam(defaultValue = "3") int limit
    ) {

        LiveItemListResponse response = liveItemService.getHotLiveItems(limit);

        return ApiResponse.ok("인기 경매품 다건 조회", response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "경매 상태 변경")
    public ApiResponse<Void> changeAuctionStatus(
        @PathVariable Long id,
        @RequestBody AuctionStatusRequest reqBody,
        @AuthenticationPrincipal CustomUserDetails userDetails

    ) {

        liveItemService.changeAuctionStatus(id, userDetails.getUser(), reqBody.auctionStatus());

        return ApiResponse.ok("%d번 경매품 경매 상태 수정".formatted(id));
    }

}