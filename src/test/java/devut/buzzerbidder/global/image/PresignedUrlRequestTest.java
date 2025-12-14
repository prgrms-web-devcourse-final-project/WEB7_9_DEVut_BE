package devut.buzzerbidder.global.image;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PresignedUrlRequestTest {

    @Test
    @DisplayName("정상 요청 - 검증 성공")
    void validateSuccess() {
        // given
        PresignedUrlRequest request = new PresignedUrlRequest(
            "test-image.jpg",
            "auctions",
            1048576L
        );

        // when & then
        assertThatCode(request::validate)
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("파일 크기 초과 - 검증 실패")
    void validateFileSizeTooLarge() {
        // given: 11MB
        PresignedUrlRequest request = new PresignedUrlRequest(
            "large-image.jpg",
            "auctions",
            11534336L
        );

        // when & then
        assertThatThrownBy(request::validate)
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("지원하지 않는 파일 형식 - 검증 실패")
    void validateInvalidFileType() {
        // given: PDF 파일
        PresignedUrlRequest request = new PresignedUrlRequest(
            "document.pdf",
            "auctions",
            1048576L
        );

        // when & then
        assertThatThrownBy(request::validate)
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_INVALID_FILE_TYPE);
    }

    @Test
    @DisplayName("확장자 없는 파일 - 검증 실패")
    void validateNoExtension() {
        // given
        PresignedUrlRequest request = new PresignedUrlRequest(
            "filename",
            "auctions",
            1048576L
        );

        // when & then
        assertThatThrownBy(request::validate)
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_INVALID_FILE_TYPE);
    }
}
