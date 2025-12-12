package devut.buzzerbidder.domain.deliveryTracking.service;

import devut.buzzerbidder.domain.deliveryTracking.infrastructure.DeliveryTrackingTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeliveryTrackingAuthService {

    @Value("${delivery.client-id}")
    private String clientId;
    @Value("${delivery.client-secret}")
    private String clientSecret;

    private final WebClient webClient;
    private final DeliveryTrackingTokenStore tokenStore;

    public synchronized String getToken() {

        if (!tokenStore.isExpired()) {
            return tokenStore.getToken();
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        Map<String, Object> response = webClient.post()
                .uri("https://auth.tracker.delivery/oauth2/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(body))
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(responseBody -> new RuntimeException("토큰 요청 실패: " + responseBody))
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        if (response == null || !response.containsKey("access_token") || !response.containsKey("expires_in")) {
            throw new IllegalStateException("토큰 발급 실패: response=" + response);
        }

        String accessToken = Optional.ofNullable((String) response.get("access_token"))
                .orElseThrow(() -> new IllegalStateException("access_token 없음"));

        Integer expiresIn = Optional.ofNullable((Integer) response.get("expires_in"))
                .orElseThrow(() -> new IllegalStateException("expires_in 없음"));

        // 토큰 저장
        tokenStore.updateToken(accessToken, expiresIn);

        return accessToken;
    }
}
