package devut.buzzerbidder.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurationSupport;

@Configuration
@EnableWebSocket
public class WebSocketConfig extends WebSocketMessageBrokerConfigurationSupport {

    // 최초 웹소켓 연결을 위한 엔드포인트 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // 임시 허용
                .setAllowedOrigins("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        // 메시지 수신 채널 설정 (SUBSCRIBE)
        config.enableSimpleBroker("/topic", "/queue");

        // 메시지 발행 채널 설정 (SEND)
        config.setApplicationDestinationPrefixes("/api");
    }

}
