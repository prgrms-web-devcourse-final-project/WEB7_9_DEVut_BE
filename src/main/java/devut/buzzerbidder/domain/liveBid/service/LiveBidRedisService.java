package devut.buzzerbidder.domain.liveBid.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor()
public class LiveBidRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis HASH에 데이터 저장
    public void setHash(String key, Map<String, String> data) {
        redisTemplate.opsForHash().putAll(key, data);
    }

    // Redis HASH에서 필드 값 조회
    public String getHashField(String key, String field) {
        Object result = redisTemplate.opsForHash().get(key, field);
        return result != null ? result.toString() : null;
    }

    // 현재 최고가보다 높을 경우에만 갱신 (원자성 보장)
    // KEYS[1]: redisKey
    // ARGV[1]: newBidPrice (새로운 입찰 가격)
    // ARGV[2]: newBidderId (새로운 입찰자 ID)
    private static final String LUA_BID_SCRIPT = """
        local maxPrice = redis.call('HGET', KEYS[1], 'maxBidPrice')
        local newPrice = tonumber(ARGV[1])

        -- maxPrice가 없거나 (초기값), 새로운 가격이 현재 최고가보다 클 때만 갱신
        if maxPrice == false or newPrice > tonumber(maxPrice) then
            redis.call('HSET', KEYS[1], 'maxBidPrice', ARGV[1])
            redis.call('HSET', KEYS[1], 'currentBidderId', ARGV[2])
            return 1 -- 성공
        else
            return 0 -- 실패 (현재 최고가보다 낮거나 같음)
        end
    """;

    // 입찰 가격 갱신
    public Long updateMaxBidPriceAtomic(String redisKey, String newBidPrice, String newBidderId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_BID_SCRIPT, Long.class);

        return redisTemplate.execute(
                script,
                Collections.singletonList(redisKey),
                newBidPrice, newBidderId
        );
    }
}
