package devut.buzzerbidder.global.config;

import devut.buzzerbidder.domain.liveBid.service.LiveBidLogConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final LiveBidLogConsumer liveBidLogConsumer;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String BID_LOG_STREAM_KEY = "auction:bid:log:stream";
    private static final String BID_LOG_GROUP = "bid-log-group";
    private static final String BID_LOG_CONSUMER_NAME = "consumer-1";

    @Bean
    public Subscription bidLogSubscription() {
        try {
            redisTemplate.opsForStream().createGroup(BID_LOG_STREAM_KEY, BID_LOG_GROUP);
            log.info("Redis Stream Consumer Group 생성 완료: stream={}, group={}", BID_LOG_STREAM_KEY, BID_LOG_GROUP);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("Redis Stream Consumer Group이 이미 존재함: stream={}, group={}", BID_LOG_STREAM_KEY, BID_LOG_GROUP);
            } else {
                log.warn("Redis Stream Consumer Group 생성 중 예외 발생: stream={}, group={}, error={}",
                        BID_LOG_STREAM_KEY, BID_LOG_GROUP, e.getMessage());
            }
        }

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        Subscription subscription = container.receive(
                Consumer.from(BID_LOG_GROUP, BID_LOG_CONSUMER_NAME),
                StreamOffset.create(BID_LOG_STREAM_KEY, ReadOffset.lastConsumed()),
                liveBidLogConsumer
        );

        container.start();
        return subscription;
    }
}