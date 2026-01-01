package devut.buzzerbidder.domain.liveBid.service;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
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
    // ARGV[3]:

    /**
    *  1: 성공(가격 갱신 + deposit 차감 + 이전 최고입찰자 환불)
    *  0: 가격 낮음/같음
    * -1: 본인이 이미 최고입찰자
    * -2: 잔액 부족(deposit 차감 불가)
    * -3: 세션 없음(auction:session:{userId} 없음)
    */
    private static final String LUA_BID_SCRIPT = """
        local liveKey = KEYS[1]
        local depositsKey = liveKey .. ':deposits'
        
        local newBidderId = tostring(ARGV[1])
        local newPrice = tonumber(ARGV[2])
        local deposit = tonumber(ARGV[3])
        local sessionTtl = tonumber(ARGV[4])
        local balanceTtl = tonumber(ARGV[5])
        
        -- wallet keys
        local sesKey = 'auction:session:' .. newBidderId
        local balKey = 'auction:bizz:' .. newBidderId
        local verKey = 'auction:bizzver:' .. newBidderId
        
        -- 세션 없으면 실패
        if redis.call('EXISTS', sesKey) == 0 then
          return -3
        end
        
        local maxPriceStr = redis.call('HGET', liveKey, 'maxBidPrice')
        local prevBidder = redis.call('HGET', liveKey, 'currentBidderId') or ''
        local curMax = 0
        if maxPriceStr and maxPriceStr ~= false then curMax = tonumber(maxPriceStr) end
        
        -- 이미 본인이 최고입찰자면 실패
        if prevBidder == newBidderId then
          return -1
        end
        
        -- deposits에 bidderId가 남아있으면(정상이라면 없어야 함) -1 반환
        if redis.call('HEXISTS', depositsKey, newBidderId) == 1 then
          return -1
        end
        
        -- 현재 최고 입찰 금액이 최소 5% 이상, 5%가 100보다 작으면 100 이상
        local inc = math.ceil(curMax * 5 / 100)
        if inc < 100 then
          inc = 100
        end
        
        local minPrice = curMax + inc
        
        if newPrice < minPrice then
          return 0
        end
        
        -- 이전 최고입찰자 환불 선체크
        if prevBidder ~= '' then
          local prevDepStr = redis.call('HGET', depositsKey, prevBidder)
          if prevDepStr and prevDepStr ~= false then
            local prevBalKey = 'auction:bizz:' .. prevBidder
            local prevVerKey = 'auction:bizzver:' .. prevBidder

            local prevBalStr = redis.call('GET', prevBalKey)
            if not prevBalStr then
              return -3
            end

            -- verKey도 없으면(세션/TTL 꼬임) 정책상 -3
            if redis.call('EXISTS', prevVerKey) == 0 then
              return -3
            end
          end
        end
        
        -- deposit 차감: 잔액 체크 후 차감
        local balStr = redis.call('GET', balKey)
        if not balStr then
          return -3
        end
        
        local bal = tonumber(balStr)
        if bal < deposit then
          return -2
        end
        
        redis.call('SET', balKey, bal - deposit)
        redis.call('INCR', verKey)
        
        -- deposits에 deposit 기록
        redis.call('HSET', depositsKey, newBidderId, tostring(deposit))
        
        -- 이전 최고입찰자 deposit 즉시 환불
        if prevBidder ~= '' then
          local prevDepStr = redis.call('HGET', depositsKey, prevBidder)
          if prevDepStr and prevDepStr ~= false then
            local prevDep = tonumber(prevDepStr)
        
            local prevBalKey = 'auction:bizz:' .. prevBidder
            local prevVerKey = 'auction:bizzver:' .. prevBidder
        
            local prevBalStr = redis.call('GET', prevBalKey)
            if not prevBalStr then
              return -3
            end
            redis.call('SET', prevBalKey, tonumber(prevBalStr) + prevDep)
            redis.call('INCR', prevVerKey)
        
            if balanceTtl and balanceTtl > 0 then
              redis.call('EXPIRE', prevBalKey, balanceTtl)
              redis.call('EXPIRE', prevVerKey, balanceTtl)
            end
        
            redis.call('HDEL', depositsKey, prevBidder)
          end
        end
        
        -- 최고가 갱신
        redis.call('HSET', liveKey, 'maxBidPrice', tostring(newPrice))
        redis.call('HSET', liveKey, 'currentBidderId', newBidderId)
        
        -- TTL 연장
        if sessionTtl and sessionTtl > 0 then
          redis.call('EXPIRE', sesKey, sessionTtl)
        end
        
        if balanceTtl and balanceTtl > 0 then
          redis.call('EXPIRE', balKey, balanceTtl)
          redis.call('EXPIRE', verKey, balanceTtl)
          redis.call('EXPIRE', depositsKey, balanceTtl)
        end
        
        return 1
""";

    @Timed(
            value = "buzzerbidder.redis.livebid",
            extraTags = {"op", "atomic_update"},
            histogram = true
    )
    public Long updateMaxBidPriceAtomicWithDeposit(
            String redisKey,
            Long bidderId,
            Integer bidPrice,
            Long depositAmount,
            Long sessionTtlSeconds,
            Long balanceTtlSeconds
    ) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_BID_SCRIPT, Long.class);

        try {
            return redisTemplate.execute(
                    script,
                    Collections.singletonList(redisKey),
                    bidderId.toString(),
                    bidPrice.toString(),
                    depositAmount.toString(),
                    sessionTtlSeconds.toString(),
                    balanceTtlSeconds.toString()
            );
        } catch (DataAccessException e) {
            // 레디스 연결/타임아웃/스크립트 실행 실패 등은 여기로 옴
            throw new IllegalStateException("Redis LUA 실행 실패. redisKey=" + redisKey + ", bidderId=" + bidderId, e);
        }
    }

}
