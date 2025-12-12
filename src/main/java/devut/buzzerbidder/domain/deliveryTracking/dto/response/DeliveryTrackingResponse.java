package devut.buzzerbidder.domain.deliveryTracking.dto.response;

import devut.buzzerbidder.domain.deliveryTracking.dto.DeliveryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record DeliveryTrackingResponse(
        DeliveryEvent lastEvent,
        List<DeliveryEvent> events
) {
    @SuppressWarnings("unchecked")
    public static DeliveryTrackingResponse from(Map<String, Object> trackMap) {
        if (trackMap == null) return null;

        DeliveryEvent lastEvent = DeliveryEvent.from((Map<String, Object>) trackMap.get("lastEvent"));

        List<DeliveryEvent> progresses = new ArrayList<>();
        Map<String, Object> eventsMap = (Map<String, Object>) trackMap.get("events");
        if (eventsMap != null && eventsMap.containsKey("edges")) {
            List<?> edgesRaw = (List<?>) eventsMap.get("edges");
            for (Object edgeObj : edgesRaw) {
                Map<String, Object> edge = (Map<String, Object>) edgeObj;
                Map<String, Object> node = (Map<String, Object>) edge.get("node");
                progresses.add(DeliveryEvent.from(node));
            }
        }

        return new DeliveryTrackingResponse(lastEvent, progresses);
    }
}
