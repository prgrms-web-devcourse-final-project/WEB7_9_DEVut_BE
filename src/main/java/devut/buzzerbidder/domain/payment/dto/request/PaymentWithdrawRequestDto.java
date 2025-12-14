package devut.buzzerbidder.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentWithdrawRequestDto(

        @NotNull
        @Positive
        Long amount,

        @NotBlank
        String bankName,

        @NotBlank
        String accountNumber,

        @NotBlank
        String accountHolder
) {
}
