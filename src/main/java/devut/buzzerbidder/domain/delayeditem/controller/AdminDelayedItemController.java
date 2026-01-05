package devut.buzzerbidder.domain.delayeditem.controller;

import devut.buzzerbidder.domain.delayeditem.dto.response.AdminDelayedItemResponseDto;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.service.AdminDelayedItemService;
import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/auction/delayed")
@Tag(name = "AdminDelayedItem", description = "관리자 지연 경매 API")
public class AdminDelayedItemController {

    private final AdminDelayedItemService adminDelayedItemService;

    @GetMapping()
    @Operation(summary = "지연 경매 상품 조회(관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDelayedItemResponseDto> getDelayedItemsForAdmin(
            @RequestParam(required = false) Long sellerUserId,
            @RequestParam(required = false)DelayedItem.Category category,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "15") Integer size
    ) {
        AdminDelayedItemResponseDto response = adminDelayedItemService.getDelayedItemsForAdmin(sellerUserId, category, page, size);
        return ApiResponse.ok("지연 경매 상품 내역 조회에 성공했습니다",response);
    }
}
