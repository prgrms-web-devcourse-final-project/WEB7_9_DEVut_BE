package devut.buzzerbidder.domain.deliveryTracking.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DeliveryTrackingConfig {

    @Bean
    public WebClient deliveryTrackerWebClient() {
        return WebClient.builder().build();
    }

}
