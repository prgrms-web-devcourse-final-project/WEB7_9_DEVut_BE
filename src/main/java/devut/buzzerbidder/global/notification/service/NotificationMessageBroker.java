package devut.buzzerbidder.global.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import devut.buzzerbidder.global.notification.dto.NotificationDto;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMessageBroker implements MessageListener {

    private static final String NOTIFICATION_CHANNEL = "notification:events";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final SseEmitterPool emitterPool;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void subscribe() {
        redisMessageListenerContainer.addMessageListener(
            this,
            new ChannelTopic(NOTIFICATION_CHANNEL)
        );
    }

    /**
     * 개인에게만 발송
     */
    public void publishToUser(Long userId, NotificationDto notification) {
        publish("user:" + userId, notification);
    }

    /**
     * 경매 참가자 전체에게 발송
     */
    public void publishToAuction(Long auctionId, NotificationDto notification) {
        publish("auction:" + auctionId, notification);
    }

    /**
     * 여러 채널에 발송
     */
    public void publishToChannels(List<String> channels, NotificationDto notification) {
        channels.forEach(channel -> publish(channel, notification));
    }

    private void publish(String channel, NotificationDto notification) {
        try {
            NotificationMessage message = new NotificationMessage(channel, notification);
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(NOTIFICATION_CHANNEL, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize notification", e);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            NotificationMessage notificationMessage = objectMapper.readValue(
                body,
                NotificationMessage.class);

            emitterPool.sendToChannel(
                notificationMessage.getChannel(),
                notificationMessage.getNotification()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to process notification from Redis", e);
        }
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    static class NotificationMessage {
        private String channel;
        private NotificationDto notification;
    }
}
