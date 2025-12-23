package devut.buzzerbidder.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    
    @Value("${frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] allowedOrigins;
                if (frontendBaseUrl.contains("buzzerbidder.shop")) {
                    allowedOrigins = new String[]{
                        "https://www.buzzerbidder.shop",
                        "https://buzzerbidder.shop"
                    };
                } else {
                    allowedOrigins = new String[]{frontendBaseUrl};
                }
                
                registry.addMapping("/**")
                    .allowedOrigins(allowedOrigins)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                    .allowedHeaders("*")
                    .allowCredentials(true);
            }
        };
    }
}
