package devut.buzzerbidder.domain.deliveryTracking.dto.request;

import java.util.Map;

public record GraphQLRequest(
        String query,
        Map<String, Object> variables
) {}
