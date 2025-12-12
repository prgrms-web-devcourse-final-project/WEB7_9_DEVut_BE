package devut.buzzerbidder.domain.deliveryTracking.service;

import devut.buzzerbidder.domain.deliveryTracking.dto.request.GraphQLRequest;
import devut.buzzerbidder.domain.deliveryTracking.dto.response.DeliveryTrackingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliveryTrackingService {

    private final WebClient webClient;
    private final DeliveryTrackingAuthService authService;

    public DeliveryTrackingResponse track(String carrierId, String trackingNumber) {
        String token = authService.getToken();

        String query = """
                query Track($carrierId: ID!, $trackingNumber: String!) {
                  track(carrierId: $carrierId, trackingNumber: $trackingNumber) {
                    lastEvent {
                      time
                      status { name }
                      description
                      location { name }
                    }
                    events(last: 10) {
                      edges {
                        node {
                          time
                          status { name }
                          description
                          location { name }
                        }
                      }
                    }
                  }
                }
                """;

        Map<String, Object> variables = Map.of(
                "carrierId", carrierId,
                "trackingNumber", trackingNumber
        );

        Map<String, Object> response = webClient.post()
                .uri("https://apis.tracker.delivery/graphql")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(new GraphQLRequest(query, variables))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (response == null || !response.containsKey("data")) {
            throw new IllegalStateException("API 응답이 없습니다: " + response);
        }

        Map<String, Object> data = castMap(response.get("data"));
        Map<String, Object> track = castMap(data.get("track"));

        if (track == null) {
            throw new IllegalStateException("track 데이터가 없습니다");
        }

        return DeliveryTrackingResponse.from(track);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalStateException("Map 변환 실패: " + obj);
    }
}
