package devut.buzzerbidder.domain.liveitem.controller;

import devut.buzzerbidder.domain.liveitem.dto.response.AdminLiveItemResponseDto;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.service.AdminLiveItemService;
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
@RequestMapping("/api/v1/admin/auction/live")
@Tag(name = "AdminLiveItem", description = "관리자 라이브 경매 API")
public class AdminLiveItemController {

    private final AdminLiveItemService adminLiveItemService;

    @GetMapping()
    @Operation(summary = "라이브 경매 상품 조회(관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminLiveItemResponseDto> getLiveItemsForAdmin(
            @RequestParam(required = false) Long sellerUserId,
            @RequestParam(required = false) LiveItem.Category category,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "15") Integer size
    ) {
        AdminLiveItemResponseDto response = adminLiveItemService.getLiveItemsForAdmin(sellerUserId, category, page, size);
        return ApiResponse.ok("지연 경매 상품 내역 조회에 성공했습니다",response);
    }
}
