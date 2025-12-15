package devut.buzzerbidder.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentConfirmRequestDto(

        @NotBlank
        String paymentKey,

        @NotBlank
        String orderId,

        @NotNull
        Long amount
) {
}
