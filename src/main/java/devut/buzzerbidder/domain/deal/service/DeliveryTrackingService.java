package devut.buzzerbidder.domain.deal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import devut.buzzerbidder.domain.deal.dto.DeliveryEvent;
import devut.buzzerbidder.domain.deal.dto.DeliveryTrackingResponse;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class DeliveryTrackingService {

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String GRAPHQL_ENDPOINT = "https://api.tracker.delivery/graphql";

    public DeliveryTrackingResponse getDeliveryInfo(
            String clientId, String clientSecret,
            String carrierId, String trackingNumber
    ) throws IOException {

        String query = """
            query {
              track(carrierId: "%s", trackingNumber: "%s") {
                lastEvent {
                  time
                  status { code text }
                  location { name }
                  description
                }
                progresses {
                  time
                  status { code text }
                  location { name }
                  description
                }
              }
            }
        """.formatted(carrierId, trackingNumber);

        String jsonPayload = objectMapper.writeValueAsString(Map.of("query", query));

        Request request = new Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .post(RequestBody.create(jsonPayload, MediaType.parse("application/json")))
                .addHeader("X-Client-Id", clientId)
                .addHeader("X-Client-Secret", clientSecret)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
            Map<String, Object> data = (Map<String, Object>) ((Map<String, Object>) map.get("data")).get("track");

            DeliveryTrackingResponse trackingResponse = new DeliveryTrackingResponse();
            trackingResponse.setLastEvent(objectMapper.convertValue(data.get("lastEvent"), DeliveryEvent.class));
            trackingResponse.setProgresses(objectMapper.convertValue(data.get("progresses"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DeliveryEvent.class)));

            return trackingResponse;
        }
    }
}
