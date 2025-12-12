package devut.buzzerbidder.domain.deliveryTracking.dto;

import java.util.Map;

public record DeliveryEvent(
        String time,
        String status,
        String locationName,
        String description
) {
    @SuppressWarnings("unchecked")
    public static DeliveryEvent from(Map<String, Object> map) {
        if (map == null) return null;

        Map<String, Object> status = (Map<String, Object>) map.get("status");
        Map<String, Object> location = (Map<String, Object>) map.get("location");

        return new DeliveryEvent(
                (String) map.get("time"),
                status != null ? (String) status.get("name") : null,
                location != null ? (String) location.get("name") : null,
                (String) map.get("description")
        );
    }
}
