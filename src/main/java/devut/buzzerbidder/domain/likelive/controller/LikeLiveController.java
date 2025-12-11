package devut.buzzerbidder.domain.likelive.controller;

import devut.buzzerbidder.domain.likelive.service.LikeLiveService;
import devut.buzzerbidder.global.response.ApiResponse;
import devut.buzzerbidder.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auction/live")
public class LikeLiveController {

    private final LikeLiveService likeLiveService;

    @PostMapping("/{id}/like")
    public ApiResponse<Boolean> toggleLike(
        @PathVariable Long id,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean liked = likeLiveService.toggleLike(userDetails.getMember().getId(),id);
        return ApiResponse.ok(liked ? "찜 완료" : "찜 취소", liked);
    }

}
