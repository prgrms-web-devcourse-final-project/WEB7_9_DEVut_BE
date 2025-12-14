package devut.buzzerbidder.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

public record PaymentHistoryRequestDto(
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        String status,

        @Min(0)
        Integer page,

        @Positive
        Integer size
) {
}
