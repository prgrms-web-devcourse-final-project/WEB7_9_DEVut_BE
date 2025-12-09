package devut.buzzerbidder.domain.deal.dto;

import java.util.List;

public record DeliveryTrackingResponse(
        DeliveryEvent lastEvent,
        List<DeliveryEvent> progresses
) {}
