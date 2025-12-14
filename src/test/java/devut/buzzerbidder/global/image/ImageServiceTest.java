package devut.buzzerbidder.global.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private ImageService imageService;

    private static final String BUCKET_NAME = "test-bucket";
    private static final String REGION = "ap-northeast-2";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imageService, "bucket", BUCKET_NAME);
        ReflectionTestUtils.setField(imageService, "region", REGION);
    }

    // ========== Presigned URL 생성 테스트 ==========
    @Test
    @DisplayName("Presigned URL 생성 성공")
    void createPresignedUrlSuccess() throws Exception {
        // given
        String fileName = "test-image.jpg";
        String directory = "auctions";

        PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
        when(mockPresignedRequest.url()).thenReturn(new URL("https://test-bucket.s3.amazonaws.com/presigned-url"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedRequest);

        // when
        PresignedUrlResponse response = imageService.createPresignedUrl(fileName, directory);

        // then
        assertThat(response).isNotNull();
        assertThat(response.presignedUrl()).isNotNull();
        assertThat(response.fileUrl()).isNotNull();
        assertThat(response.fileUrl()).contains("auctions/");
    }

    @Test
    @DisplayName("확장자 없는 파일로 Presigned URL 생성")
    void createPresignedUrlWithoutExtension() throws Exception {
        // given
        String fileName = "test-image";
        String directory = "profiles";

        PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
        when(mockPresignedRequest.url()).thenReturn(new URL("https://test-bucket.s3.amazonaws.com/presigned-url"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedRequest);

        // when
        PresignedUrlResponse response = imageService.createPresignedUrl(fileName, directory);

        // then
        assertThat(response).isNotNull();
        assertThat(response.fileUrl()).contains("profiles/");
    }

    // ========== 파일 URL 생성 테스트 ==========
    @Test
    @DisplayName("S3 파일 URL 생성 검증")
    void getFileUrlSuccess() {
        // given
        String key = "auctions/abc123.jpg";

        // when
        String fileUrl = imageService.getFileUrl(key);

        // then
        assertThat(fileUrl).isEqualTo("https://test-bucket.s3.ap-northeast-2.amazonaws.com/auctions/abc123.jpg");
    }

    // ========== 단일 파일 삭제 테스트 ==========
    @Test
    @DisplayName("단일 파일 삭제 성공")
    void deleteFileSingleSuccess() {
        // given
        String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/auctions/abc123.jpg";

        // when
        imageService.deleteFile(fileUrl);

        // then
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("단일 파일 삭제 실패 - 잘못된 URL 형식")
    void deleteFileInvalidUrl() {
        // given
        String invalidUrl = "https://invalid-url.com/image.jpg";

        // when & then
        assertThatThrownBy(() -> imageService.deleteFile(invalidUrl))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_URL_NOT_ALLOWED);
    }

    @Test
    @DisplayName("단일 파일 삭제 실패 - 빈 URL")
    void deleteFileEmptyUrl() {
        // given
        String emptyUrl = "";

        // when & then
        assertThatThrownBy(() -> imageService.deleteFile(emptyUrl))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_URL_INVALID);
    }

    @Test
    @DisplayName("단일 파일 삭제 실패 - null URL")
    void deleteFileNullUrl() {
        // given
        String nullUrl = null;

        // when & then
        assertThatThrownBy(() -> imageService.deleteFile(nullUrl))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_URL_INVALID);
    }

    @Test
    @DisplayName("단일 파일 삭제 실패 - 다른 버킷 URL")
    void deleteFileDifferentBucket() {
        // given
        String otherBucketUrl = "https://other-bucket.s3.ap-northeast-2.amazonaws.com/image.jpg";

        // when & then
        assertThatThrownBy(() -> imageService.deleteFile(otherBucketUrl))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_URL_NOT_ALLOWED);
    }

    // ========== 다중 파일 삭제 테스트 ==========
    @Test
    @DisplayName("다중 파일 삭제 성공")
    void deleteFilesMultipleSuccess() {
        // given
        List<String> fileUrls = Arrays.asList(
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/auctions/abc123.jpg",
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/auctions/def456.jpg",
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/auctions/ghi789.jpg"
        );

        // when
        imageService.deleteFiles(fileUrls);

        // then
        verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    @DisplayName("다중 파일 삭제 - 빈 리스트")
    void deleteFilesEmptyList() {
        // given
        List<String> emptyList = Collections.emptyList();

        // when
        imageService.deleteFiles(emptyList);

        // then
        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    @DisplayName("다중 파일 삭제 - null 리스트")
    void deleteFilesNullList() {
        // given
        List<String> nullList = null;

        // when
        imageService.deleteFiles(nullList);

        // then
        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    @DisplayName("다중 파일 삭제 실패 - 잘못된 URL 포함")
    void deleteFilesInvalidUrlInList() {
        // given
        List<String> fileUrls = Arrays.asList(
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/auctions/abc123.jpg",
            "https://invalid-url.com/image.jpg",
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/auctions/ghi789.jpg"
        );

        // when & then
        assertThatThrownBy(() -> imageService.deleteFiles(fileUrls))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_URL_NOT_ALLOWED);
    }

    // ========== URL 검증 추가 테스트 ==========
    @Test
    @DisplayName("URL 검증 - 다른 리전")
    void validateUrlDifferentRegion() {
        // given
        String differentRegionUrl = "https://test-bucket.s3.us-east-1.amazonaws.com/image.jpg";

        // when & then
        assertThatThrownBy(() -> imageService.deleteFile(differentRegionUrl))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_URL_NOT_ALLOWED);
    }

    @Test
    @DisplayName("URL 검증 - HTTP 프로토콜 (HTTPS 아님)")
    void validateUrlHttpProtocol() {
        // given
        String httpUrl = "http://test-bucket.s3.ap-northeast-2.amazonaws.com/image.jpg";

        // when & then
        assertThatThrownBy(() -> imageService.deleteFile(httpUrl))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_URL_NOT_ALLOWED);
    }

    @Test
    @DisplayName("URL 검증 - 공백 문자열")
    void validateUrlBlankString() {
        // given
        String blankUrl = "   ";

        // when & then
        assertThatThrownBy(() -> imageService.deleteFile(blankUrl))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_URL_INVALID);
    }
}
