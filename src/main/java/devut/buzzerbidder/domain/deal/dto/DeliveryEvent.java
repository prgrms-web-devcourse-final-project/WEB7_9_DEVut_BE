package devut.buzzerbidder.domain.deal.dto;

public record DeliveryEvent(
        String time,
        String statusCode,
        String statusText,
        String locationName,
        String description
) {}
