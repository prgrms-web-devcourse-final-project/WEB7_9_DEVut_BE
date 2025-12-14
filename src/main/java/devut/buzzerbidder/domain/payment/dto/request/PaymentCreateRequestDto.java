package devut.buzzerbidder.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record PaymentCreateRequestDto(

        @NotNull
        Long amount
) {
}
