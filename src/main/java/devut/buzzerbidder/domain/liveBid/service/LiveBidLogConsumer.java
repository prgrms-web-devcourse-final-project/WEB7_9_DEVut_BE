package devut.buzzerbidder.domain.liveBid.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

/**
 * 비동기 저장 흐름:
 * 1. Redis Stream에서 메시지 수신
 * 2. LiveBidLogPersistService를 통해 DB 저장 (재시도 + 트랜잭션 적용)
 * 3. 저장 성공 시 ACK 전송
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveBidLogConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final LiveBidLogPersistService liveBidLogPersistService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String BID_LOG_STREAM_KEY = "auction:bid:log:stream";
    private static final String BID_LOG_GROUP = "bid-log-group";

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            liveBidLogPersistService.processAndSave(message);

            // 비즈니스 로직이 예외 없이 성공했을 때만 ACK를 전송
            redisTemplate.opsForStream().acknowledge(BID_LOG_STREAM_KEY, BID_LOG_GROUP, message.getId());
            log.debug("입찰 로그 처리 완료 및 ACK 전송: msgId={}", message.getId());

        } catch (Exception e) {
            // 3회 재시도 후에도 실패한 경우
            log.error("입찰 로그 최종 저장 실패. MsgId: {}, Error: {}", message.getId(), e.getMessage());
        }
    }

}