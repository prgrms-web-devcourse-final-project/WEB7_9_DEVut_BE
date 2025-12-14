package devut.buzzerbidder.global.image;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Tag(name = "Image", description = "이미지 업로드 API")
public class ImageController {

    private final ImageService imageService;
    private final RequestContext requestContext;

    @Operation(
        summary = "Presigned URL 발급",
        description = "S3 직접 업로드를 위한 Presigned URL을 생성합니다. " +
            "클라이언트는 받은 presignedUrl로 PUT 요청하여 파일을 직접 업로드하고, " +
            "업로드 완료 후 fileUrl을 서버에 전달합니다."
    )
    @PostMapping("/upload")
    public ApiResponse<PresignedUrlResponse> generatePresignedUrl(
        @Valid @RequestBody PresignedUrlRequest request) {

        // 인증 확인
        User user = requestContext.getCurrentUser();

        // 요청 검증
        request.validate();

        // Presigned URL 생성
        PresignedUrlResponse response = imageService.createPresignedUrl(
            request.fileName(),
            request.directory()
        );

        return ApiResponse.ok("Presigned URL 생성 성공", response);
    }
}
