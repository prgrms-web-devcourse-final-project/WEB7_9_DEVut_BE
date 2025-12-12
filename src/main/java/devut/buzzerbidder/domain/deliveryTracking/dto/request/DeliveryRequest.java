package devut.buzzerbidder.domain.deliveryTracking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DeliveryRequest(
        @Schema(description = "택배사 코드", example = "kr.cjlogistics")
        @NotBlank(message = "택배사 코드는 필수입니다.")
        String carrierCode,
        @Schema(description = "운송장 번호", example = "1234567890")
        @NotBlank(message = "운송장 번호는 필수입니다.")
        String trackingNumber
) {}
