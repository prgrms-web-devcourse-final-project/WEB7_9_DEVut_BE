package devut.buzzerbidder.domain.user.dto.response;

import devut.buzzerbidder.domain.user.entity.DeliveryAddress;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "배송지 정보")
public record DeliveryAddressResponse(
        @Schema(description = "배송지 ID", example = "1")
        Long id,

        @Schema(description = "주소", example = "서울시 중구 세종대로 135-5")
        String address,

        @Schema(description = "상세주소", example = "OO아파트 101동 101호")
        String addressDetail,

        @Schema(description = "우편번호", example = "12345")
        String postalCode,

        @Schema(description = "기본 배송지 여부", example = "true")
        Boolean isDefault
) {
    public static DeliveryAddressResponse from(DeliveryAddress deliveryAddress) {
        return new DeliveryAddressResponse(
                deliveryAddress.getId(),
                deliveryAddress.getAddress(),
                deliveryAddress.getAddressDetail(),
                deliveryAddress.getPostalCode(),
                deliveryAddress.getIsDefault()
        );
    }
}

