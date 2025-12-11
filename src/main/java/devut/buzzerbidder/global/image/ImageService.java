package devut.buzzerbidder.global.image;

import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
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

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    private final S3Presigner s3Presigner;


    /** Presigned URL + 최종 URL 함께 생성 */
    public PresignedUrlResponse createPresignedUrl(String fileName, String directory) {
        log.info("Presigned URL 생성 요청: fileName={}, directory={}", fileName, directory);

        String key = generateKey(fileName, directory);
        String extension = getFileExtension(fileName);
        String contentType = getContentType(extension);
        String presignedUrl = generatePresignedUrl(key, contentType);
        String finalUrl = getFileUrl(key);

        log.info("Presigned URL 생성 완료: key={}, contentType={}", key, contentType);
        return new PresignedUrlResponse(presignedUrl, finalUrl);
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

    /** 파일 확장자에 맞는 Content-Type 반환 */
    private String getContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    /** S3 객체 키 생성 (디렉토리/UUID.확장자) */
    private String generateKey(String fileName, String directory) {
        String extension = getFileExtension(fileName);
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
        log.warn("파일 삭제 요청: url={}", fileUrl);
        String key = extractNameFromUrl(fileUrl);

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("파일 삭제 완료: key={}", key);
    }

    /** S3에서 여러 파일 삭제 */
    public void deleteFiles(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return;
        }

        // URL -> S3 키로 변경
        List<ObjectIdentifier> objectidentifiers = fileUrls.stream()
            .map(this::extractNameFromUrl)
            .map(key -> ObjectIdentifier.builder().key(key).build())
            .toList();

        // 한 번의 요청으로 여러 파일 삭제
        Delete delete = Delete.builder()
            .objects(objectidentifiers)
            .build();

        DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
            .bucket(bucket)
            .delete(delete)
            .build();

        s3Client.deleteObjects(deleteObjectsRequest);
    }

    /** 파일 확장자 추출 */
    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf('.');
        if (lastIndexOf == -1) {
            return "";
        }
        return filename.substring(lastIndexOf + 1);
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
