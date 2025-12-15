package devut.buzzerbidder.global.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DopplerContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String DOPPLER_API_URL = "https://api.doppler.com/v3";
    private static final int TIMEOUT_SECONDS = 10;

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        
        // Doppler 활성화 여부 확인
        String enabled = environment.getProperty("doppler.enabled", "false");
        if (!Boolean.parseBoolean(enabled)) {
            log.info("Doppler is disabled. Skipping Doppler secret loading.");
            return;
        }

        // Doppler 설정 가져오기
        String token = environment.getProperty("doppler.token");
        String project = environment.getProperty("doppler.project");
        String config = environment.getProperty("doppler.config");

        if (token == null || token.isEmpty()) {
            log.warn("Doppler token is not set. Skipping Doppler secret loading.");
            return;
        }

        if (project == null || project.isEmpty()) {
            log.warn("Doppler project is not set. Skipping Doppler secret loading.");
            return;
        }

        if (config == null || config.isEmpty()) {
            log.warn("Doppler config is not set. Skipping Doppler secret loading.");
            return;
        }

        try {
            log.info("Loading secrets from Doppler: project={}, config={}", project, config);
            Map<String, Object> secrets = fetchSecretsFromDoppler(token, project, config);
            
            if (secrets != null && !secrets.isEmpty()) {
                MutablePropertySources propertySources = environment.getPropertySources();
                propertySources.addFirst(new MapPropertySource("doppler", secrets));
                log.info("Successfully loaded {} secrets from Doppler", secrets.size());
            } else {
                log.warn("No secrets found in Doppler");
            }
        } catch (Exception e) {
            log.error("Failed to load secrets from Doppler: {}", e.getMessage(), e);
            // Doppler 실패 시에도 애플리케이션은 계속 실행되도록 함
        }
    }

    private Map<String, Object> fetchSecretsFromDoppler(String token, String project, String config) {
        WebClient webClient = WebClient.builder()
                .baseUrl(DOPPLER_API_URL)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();

        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/configs/config/secrets/download")
                            .queryParam("project", project)
                            .queryParam("config", config)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(TIMEOUT_SECONDS));

            if (response == null || response.isEmpty()) {
                log.warn("Empty response from Doppler API");
                return null;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response);

            Map<String, Object> secrets = new HashMap<>();
            if (jsonNode.isObject()) {
                jsonNode.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    JsonNode value = entry.getValue();
                    
                    // Doppler의 응답 형식에 따라 처리
                    // 일반적으로 "computed" 또는 "raw" 필드가 있음
                    if (value.isObject()) {
                        if (value.has("computed")) {
                            secrets.put(key, value.get("computed").asText());
                        } else if (value.has("raw")) {
                            secrets.put(key, value.get("raw").asText());
                        } else {
                            secrets.put(key, value.asText());
                        }
                    } else {
                        secrets.put(key, value.asText());
                    }
                });
            }

            return secrets;
        } catch (WebClientResponseException e) {
            log.error("Doppler API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch secrets from Doppler: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while fetching secrets from Doppler", e);
            throw new RuntimeException("Failed to fetch secrets from Doppler: " + e.getMessage(), e);
        }
    }
}

