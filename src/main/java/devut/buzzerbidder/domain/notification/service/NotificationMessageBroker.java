package devut.buzzerbidder.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import devut.buzzerbidder.domain.notification.dto.NotificationDto;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
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
    @Retryable(
        retryFor = {RedisConnectionFailureException.class, DataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishToUser(Long userId, NotificationDto notification) {
        publish("user:" + userId, notification);
    }

    /**
     * 경매 참가자 전체에게 발송
     */
    @Retryable(
        retryFor = {RedisConnectionFailureException.class, DataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishToAuction(Long auctionId, NotificationDto notification) {
        publish("auction:" + auctionId, notification);
    }

    /**
     * 여러 채널에 발송
     */
    @Retryable(
        retryFor = {RedisConnectionFailureException.class, DataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publishToChannels(List<String> channels, NotificationDto notification) {
        channels.forEach(channel -> publish(channel, notification));
    }

    private void publish(String channel, NotificationDto notification) {
        try {
            NotificationMessage message = new NotificationMessage(channel, notification);
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(NOTIFICATION_CHANNEL, json);
        } catch (JsonProcessingException e) {
            log.error("알림 직렬화 실패: channel={}, notification={}", channel, notification, e);
            throw new RuntimeException("알림 직렬화 실패", e);
        }
    }

    @Recover
    public void recoverPublishToUser(Exception e, Long userId, NotificationDto notification) {
        log.error("알림 발송 최종 실패 - userId: {}, type: {}, message: {}, error: {}",
            userId, notification.type(), notification.message(), e.getMessage());
    }

    @Recover
    public void recoverPublishToAuction(Exception e, Long auctionId, NotificationDto notification) {
        log.error("경매 알림 발송 최종 실패 - auctionId: {}, type: {}, error: {}",
            auctionId, notification.type(), e.getMessage());
    }

    @Recover
    public void recoverPublishToChannels(Exception e, List<String> channels, NotificationDto notification) {
        log.error("다중 채널 알림 발송 최종 실패 - channels: {}, type: {}, error: {}",
            channels.size(), notification.type(), e.getMessage());
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
            log.error("Redis 메시지 처리 실패: message={}", new String(message.getBody()), e);
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
