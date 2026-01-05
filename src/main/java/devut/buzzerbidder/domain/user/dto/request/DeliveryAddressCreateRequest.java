package devut.buzzerbidder.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "배송지 추가 요청")
public record DeliveryAddressCreateRequest(
        @Schema(description = "주소", example = "서울시 중구 세종대로 135-5")
        @NotBlank(message = "주소는 필수입니다.")
        String address,

        @Schema(description = "상세주소", example = "OO아파트 101동 101호")
        @NotBlank(message = "상세주소는 필수입니다.")
        String addressDetail,

        @Schema(description = "우편번호", example = "12345")
        @NotBlank(message = "우편번호는 필수입니다.")
        String postalCode,

        @Schema(description = "기본 배송지로 설정 여부", example = "true")
        Boolean isDefault
) {
}

