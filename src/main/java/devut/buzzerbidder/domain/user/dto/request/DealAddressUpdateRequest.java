package devut.buzzerbidder.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래 배송지 주소 수정 요청")
public record DealAddressUpdateRequest(
        @Schema(description = "주소", example = "서울시 중구 세종대로 135-5")
        String address,

        @Schema(description = "상세주소", example = "OO아파트 101동 101호")
        String addressDetail,

        @Schema(description = "우편번호", example = "12345")
        String postalCode
) {
}

