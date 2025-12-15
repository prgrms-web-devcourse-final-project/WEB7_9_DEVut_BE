package devut.buzzerbidder.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentFailRequestDto(

        @NotBlank
        String orderId,

        @NotBlank
        String code,

        @NotBlank
        String msg
) {
}
