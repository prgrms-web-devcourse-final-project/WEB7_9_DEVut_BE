package devut.buzzerbidder.domain.payment.dto.response;

import devut.buzzerbidder.domain.payment.dto.PaymentHistoryItemDto;
import devut.buzzerbidder.domain.payment.entity.Payment;
import org.springframework.data.domain.Page;

import java.util.List;

public record PaymentHistoryResponseDto(
        List<PaymentHistoryItemDto> payments,
        Integer totalPages,
        Long totalElements,
        Integer currentPages
) {
    public static PaymentHistoryResponseDto from(List<PaymentHistoryItemDto> payments, Page<Payment> paymentPage) {
        return new PaymentHistoryResponseDto(
                payments,
                paymentPage.getTotalPages(),
                paymentPage.getTotalElements(),
                paymentPage.getNumber()
        );
    }
}
