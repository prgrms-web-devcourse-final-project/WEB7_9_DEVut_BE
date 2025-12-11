package devut.buzzerbidder.global.image;

import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "Presigned URL 발급", description = "S3 직접 업로드를 위한 Presigned URL을 생성합니다.")
    @PostMapping("/upload")
    public ApiResponse<PresignedUrlResponse> generatePresignedUrl(
        @RequestBody PresignedUrlRequest request) {

        PresignedUrlResponse response = imageService.createPresignedUrl(
            request.fileName(),
            request.directory()
        );

        return ApiResponse.ok("Presigned URL 생성 성공", response);
    }
}
