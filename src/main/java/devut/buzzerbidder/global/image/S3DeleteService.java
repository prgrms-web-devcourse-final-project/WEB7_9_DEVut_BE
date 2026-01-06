package devut.buzzerbidder.global.image;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3DeleteService {

    private final S3Client s3Client;

    /**
     * 단일 개체 삭제
     */
    @CircuitBreaker(name = "s3DeleteCircuit", fallbackMethod = "deleteObjectFallback")
    @Retry(name = "s3DeleteRetry")
    public void deleteObject(String bucket, String key) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
        );
    }

    public void deleteObjectFallback(String bucket, String key, Throwable t) {
        log.warn("S3 delete fallback triggered. bucket={}, key={}", bucket, key, t);
    }

    /**
     * 다중 개체 삭제
     */
    @CircuitBreaker(name = "s3DeleteCircuit", fallbackMethod = "deleteObjectsFallback")
    @Retry(name = "s3DeleteRetry")
    public void deleteObjects(String bucket, List<String> keys) {

        if (keys == null || keys.isEmpty()) {
            return;
        }

        List<ObjectIdentifier> identifiers = keys.stream()
            .map(key -> ObjectIdentifier.builder().key(key).build())
            .toList();

        Delete delete = Delete.builder()
            .objects(identifiers)
            .build();

        DeleteObjectsRequest request = DeleteObjectsRequest.builder()
            .bucket(bucket)
            .delete(delete)
            .build();

        s3Client.deleteObjects(request);
    }

    public void deleteObjectsFallback(String bucket, List<String> keys, Throwable t) {
        log.warn("S3 bulk delete skipped. keys={}", keys, t);
    }


}
