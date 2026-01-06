package devut.buzzerbidder.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentCancelRequestDto(

        @NotBlank
        String cancelReason
) {
}
