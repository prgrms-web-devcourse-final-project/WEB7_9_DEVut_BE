package devut.buzzerbidder.global.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "TeamA4|DEVut BuzzerBidder API",
        description = "TeamA4|DEVut의 프로젝트 BuzzerBidder REST API 문서"
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new io.swagger.v3.oas.models.info.Info()
                .title("TeamA4|DEVut BuzzerBidder API")
                .version("1.0.0")
                .description("TeamA4|DEVut의 프로젝트 BuzzerBidder REST API 문서입니다.")
            );
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
            .group("01. User API")
            .pathsToMatch("/api/v1/users/**")
            .build();
    }

    @Bean
    public GroupedOpenApi userMeApi() {
        return GroupedOpenApi.builder()
            .group("02. User Me API")
            .pathsToMatch("/api/v1/users/me/**")
            .build();
    }

    @Bean
    public GroupedOpenApi liveItemApi() {
        return GroupedOpenApi.builder()
            .group("03. Live Item API")
            .pathsToMatch("/api/v1/auction/live/**")
            .build();
    }

    @Bean
    public GroupedOpenApi liveBidApi() {
        return GroupedOpenApi.builder()
            .group("04. Live Bid API")
            .pathsToMatch("/api/v1/live-bids/**")
            .build();
    }

    @Bean
    public GroupedOpenApi chatRoomApi() {
        return GroupedOpenApi.builder()
            .group("05. Chat Room API")
            .pathsToMatch("/api/v1/chatrooms/**")
            .build();
    }

    @Bean
    public GroupedOpenApi paymentApi() {
        return GroupedOpenApi.builder()
            .group("06. Payment API")
            .pathsToMatch("/api/v1/payments/**")
            .build();
    }

    @Bean
    public GroupedOpenApi walletApi() {
        return GroupedOpenApi.builder()
            .group("07. Wallet API")
            .pathsToMatch("/api/v1/wallets/**")
            .build();
    }

    @Bean
    public GroupedOpenApi imageApi() {
        return GroupedOpenApi.builder()
            .group("08. Image API")
            .pathsToMatch("/api/v1/images/**")
            .build();
    }

    @Bean
    public GroupedOpenApi liveDealApi() {
        return GroupedOpenApi.builder()
            .group("09. Live Deal API")
            .pathsToMatch("/api/v1/user/me/**")
            .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
            .group("00. All APIs")
            .pathsToMatch("/api/v1/**")
            .build();
    }

}