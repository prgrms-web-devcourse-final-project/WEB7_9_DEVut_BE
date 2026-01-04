package devut.buzzerbidder.global.image;

import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * AWS S3 이미지 Presigned URL 및 삭제 공통 서비스
 *
 * <p>Presigned URL을 통한 클라이언트 직접 업로드 방식 사용
 * <p>업로드된 파일은 UUID로 고유한 이름 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final S3Client s3Client;
    private final S3DeleteService s3DeleteService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    private final S3Presigner s3Presigner;


    /** Presigned URL + 최종 URL 함께 생성 */
    @Retry(
        name = "s3PresignRetry",
        fallbackMethod = "createPresignedUrlFallback"
    )
    public PresignedUrlResponse createPresignedUrl(String fileName, String directory) {
        String key = generateKey(fileName, directory);
        String extension = ImageFileUtils.getFileExtension(fileName);
        String contentType = ImageFileUtils.getContentType(extension);
        String presignedUrl = generatePresignedUrl(key, contentType);
        String finalUrl = getFileUrl(key);

        return new PresignedUrlResponse(presignedUrl, finalUrl);
    }

    private PresignedUrlResponse createPresignedUrlFallback(String fileName, String directory, Throwable t) {
        log.error("Failed to create presigned url", t);
        throw new BusinessException(ErrorCode.IMAGE_PRESIGNED_URL_FAILED);
    }

    /** S3에 PUT 요청할 수 있는 임시 URL 생성 */
    private String generatePresignedUrl(String key, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .putObjectRequest(putObjectRequest)
            .signatureDuration(Duration.ofMinutes(10))
            .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /** S3 객체 키 생성 (디렉토리/UUID.확장자) */
    private String generateKey(String fileName, String directory) {
        String extension = ImageFileUtils.getFileExtension(fileName);
        String uuid = UUID.randomUUID().toString();
        return directory + "/" + uuid + "." + extension;
    }

    /** S3 객체의 공개 접근 URL 생성 */
    public String getFileUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    /**
     * S3에서 단일 파일 삭제
     * @param fileUrl 삭제할 파일의 S3 URL
     */
    public void deleteFile(String fileUrl) {
        String key = extractNameFromUrl(fileUrl);
        s3DeleteService.deleteObject(bucket, key);
    }

    /** S3에서 여러 파일 삭제 */
    public void deleteFiles(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return;
        }

        List<String> keys = fileUrls.stream()
            .map(this::extractNameFromUrl)
            .toList();

        s3DeleteService.deleteObjects(bucket, keys);
    }

    /** S3 URL에서 키(경로) 추출 및 검증 */
    private String extractNameFromUrl(String fileUrl) {
        validateS3Url(fileUrl);

        String expectedPrefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        return fileUrl.substring(expectedPrefix.length());
    }

    /** S3 URL 형식 및 버킷 검증 */
    private void validateS3Url(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new BusinessException(ErrorCode.IMAGE_URL_INVALID);
        }

        String expectedPrefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";

        if (!fileUrl.startsWith(expectedPrefix)) {
            throw new BusinessException(ErrorCode.IMAGE_URL_NOT_ALLOWED);
        }
    }
}
