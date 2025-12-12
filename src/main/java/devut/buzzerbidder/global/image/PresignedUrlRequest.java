package devut.buzzerbidder.global.image;

import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Presigned URL 발급 요청 DTO
 */
public record PresignedUrlRequest(

    @Schema(description = "파일명", example = "profile.jpg")
    @NotBlank(message = "파일명은 필수입니다.")
    String fileName,

    @Schema(description = "업로드 디렉토리", example = "auctions")
    @NotBlank(message = "디렉토리는 필수입니다.")
    String directory,

    @Schema(description = "파일 크기 (bytes)", example = "1048576")
    @NotNull(message = "파일 크기는 필수입니다.")
    Long fileSize
) {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 요청 검증
     */
    public void validate() {
        // 파일 크기 검증
        if (fileSize > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.IMAGE_FILE_TOO_LARGE);
        }

        // 파일 확장자 검증
        String extension = ImageFileUtils.getFileExtension(fileName);
        if (!ImageFileUtils.isSupportedExtension(extension)) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_FILE_TYPE);
        }
    }
}

