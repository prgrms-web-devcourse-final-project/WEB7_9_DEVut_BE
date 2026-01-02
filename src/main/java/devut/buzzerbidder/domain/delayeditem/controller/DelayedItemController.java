package devut.buzzerbidder.domain.delayeditem.controller;

import devut.buzzerbidder.domain.delayeditem.dto.request.DelayedItemCreateRequest;
import devut.buzzerbidder.domain.delayeditem.dto.request.DelayedItemModifyRequest;
import devut.buzzerbidder.domain.delayeditem.dto.request.DelayedItemSearchRequest;
import devut.buzzerbidder.domain.delayeditem.dto.response.DelayedItemDetailResponse;
import devut.buzzerbidder.domain.delayeditem.dto.response.DelayedItemListResponse;
import devut.buzzerbidder.domain.delayeditem.dto.response.DelayedItemResponse;
import devut.buzzerbidder.domain.delayeditem.service.DelayedItemService;
import devut.buzzerbidder.global.response.ApiResponse;
import devut.buzzerbidder.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auction/delayed")
@Tag(name = "DelayedItem", description = "지연 경매 API")
public class DelayedItemController {

    private final DelayedItemService delayedItemService;

    @PostMapping
    @Operation(summary = "지연 경매 생성")
    public ApiResponse<DelayedItemResponse> createDelayedItem(
        @Valid @RequestBody DelayedItemCreateRequest reqbody,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DelayedItemResponse response = delayedItemService.writeDelayedItem(reqbody, userDetails.getUser());

        return ApiResponse.ok("지연 경매 생성", response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "지연 경매 정보 수정")
    public ApiResponse<DelayedItemResponse> modifyDelayedItem(
        @PathVariable Long id,
        @Valid @RequestBody DelayedItemModifyRequest reqBody,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DelayedItemResponse response = delayedItemService.modifyDelayedItem(id, reqBody, userDetails.getUser());

        return ApiResponse.ok("%d번 지연 경매 수정".formatted(id), response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "지연 경매 삭제")
    public ApiResponse<Void> deleteDelayedItem(
        @PathVariable Long id,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        delayedItemService.deleteDelayedItem(id, userDetails.getUser());

        return ApiResponse.ok("%d번 지연 경매 삭제".formatted(id));
    }

    @GetMapping
    @Operation(summary = "지연 경매 목록 조회")
    public ApiResponse<DelayedItemListResponse> getDelayedItems(
        DelayedItemSearchRequest reqBody,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "15") int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Long userId = userDetails != null ? userDetails.getUser().getId() : null;

        DelayedItemListResponse response =
            delayedItemService.getDelayedItems(reqBody, pageable, userId);

        return ApiResponse.ok("지연 경매 목록 조회", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "지연 경매 상세 조회")
    public ApiResponse<DelayedItemDetailResponse> getDelayedItem(
        @PathVariable Long id,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        DelayedItemDetailResponse response = delayedItemService.getDelayedItem(id, userId);

        return ApiResponse.ok("%d번 지연 경매 상세 조회".formatted(id), response);
    }

    @GetMapping("/hot")
    @Operation(summary = "인기 지연 경매 조회")
    public ApiResponse<DelayedItemListResponse> getHotDelayedItems(
        @RequestParam(defaultValue = "4") int limit
    ) {
        DelayedItemListResponse response = delayedItemService.getHotDelayedItems(limit);

        return ApiResponse.ok("인기 지연 경매 조회", response);
    }

    @GetMapping("/most-bidded")
    @Operation(summary = "입찰 경쟁 지연 경매 조회")
    public ApiResponse<DelayedItemListResponse> getMostBiddedDelayedItems(
        @RequestParam(defaultValue = "4") int limit
    ) {
        DelayedItemListResponse response = delayedItemService.getMostBiddedDelayedItems(limit);

        return ApiResponse.ok("입찰 경쟁 지연 경매 조회", response);
    }
}
