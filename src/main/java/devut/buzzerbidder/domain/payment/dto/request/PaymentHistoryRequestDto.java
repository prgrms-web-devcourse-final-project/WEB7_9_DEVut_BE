package devut.buzzerbidder.domain.payment.dto.request;

import java.time.OffsetDateTime;

public record PaymentHistoryRequestDto(
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        String status,
        Integer page,
        Integer size
) {
}
