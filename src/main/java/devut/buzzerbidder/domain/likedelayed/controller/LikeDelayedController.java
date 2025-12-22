package devut.buzzerbidder.domain.likedelayed.controller;

import devut.buzzerbidder.domain.likedelayed.service.LikeDelayedService;
import devut.buzzerbidder.global.response.ApiResponse;
import devut.buzzerbidder.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auction/delayed")
@Tag(name = "LikeDelayed", description = "지연 경매 찜 API")
public class LikeDelayedController {

    private final LikeDelayedService likeDelayedService;

    @PostMapping("/{id}/like")
    @Operation(summary = "찜 토글")
    public ApiResponse<Boolean> toggleLike(
        @PathVariable Long id,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean liked = likeDelayedService.toggleLike(userDetails.getUser().getId(), id);
        return ApiResponse.ok(liked ? "찜 완료" : "찜 취소", liked);
    }
}
