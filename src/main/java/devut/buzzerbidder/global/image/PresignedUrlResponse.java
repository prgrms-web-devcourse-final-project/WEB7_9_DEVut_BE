package devut.buzzerbidder.global.image;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Presigned URL 응답")
public record PresignedUrlResponse(
    @Schema(description = "업로드용 임시 URL (10분 유효)", example = "https://bucket.s3.amazonaws.com/...")
    String presignedUrl,

    @Schema(description = "업로드 완료 후 접근할 최종 URL", example = "https://bucket.s3.amazonaws.com/auctions/uuid.jpg")
    String fileUrl
) {
}