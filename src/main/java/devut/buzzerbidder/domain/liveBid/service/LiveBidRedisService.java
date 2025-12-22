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
        local currentBidder = redis.call("HGET", KEYS[1], 'currentBidderId')
        local newPrice = tonumber(ARGV[1])
        local newBidder = ARGV[2]
       \s
        -- 본인이 현재 최고 입찰자인지 확인
        if currentBidder == newBidder then
            return -1
        end

        -- maxPrice가 없거나 (초기값), 새로운 가격이 현재 최고가보다 클 때만 갱신
        if maxPrice == false or newPrice > tonumber(maxPrice) then
            redis.call('HSET', KEYS[1], 'maxBidPrice', ARGV[1])
            redis.call('HSET', KEYS[1], 'currentBidderId', ARGV[2])
            return 1 -- 성공
        else
            return 0 -- 실패 (현재 최고가보다 낮거나 같음)
        end
   \s""";

    /**
    *  1: 성공(가격 갱신 + deposit 차감 + 이전 최고입찰자 환불)
    *  0: 가격 낮음/같음
    * -1: 본인이 이미 최고입찰자
    * -2: 잔액 부족(deposit 차감 불가)
    * -3: 세션 없음(auction:session:{userId} 없음)
    */
    private static final String LUA_BID_WITH_DEPOSIT_SCRIPT = """
        local liveKey = KEYS[1]
        local depositsKey = liveKey .. ':deposits'
        
        local bidderId = tostring(ARGV[1])
        local newPrice = tonumber(ARGV[2])
        local deposit = tonumber(ARGV[3])
        local sessionTtl = tonumber(ARGV[4])
        local balanceTtl = tonumber(ARGV[5])
        
        -- wallet keys (참고용 WalletRedisService 규칙)
        local sesKey = 'auction:session:' .. bidderId
        local balKey = 'auction:bizz:' .. bidderId
        local verKey = 'auction:bizzver:' .. bidderId
        
        -- 세션 없으면 실패
        if redis.call('EXISTS', sesKey) == 0 then
          return -3
        end
        
        local maxPriceStr = redis.call('HGET', liveKey, 'maxBidPrice')
        local curBidder = redis.call('HGET', liveKey, 'currentBidderId') or ''
        local curMax = 0
        if maxPriceStr and maxPriceStr ~= false then curMax = tonumber(maxPriceStr) end
        
        -- 이미 본인이 최고입찰자면 실패
        if curBidder == bidderId then
          return -1
        end
        
        -- 현재 최고 입찰 금액이 최소 5% 이상, 5%가 100보다 작으면 100 이상
        local inc = math.floor(curMax * 5 / 100)
        if inc < 100 then
          inc = 100
        end
        
        local minPrice = curMax + inc
        
        if newPrice < minPrice then
          return 0
        end
        
        
        -- (중복 안전) 기존에 deposits에 남아있으면 "차액"만 차감
        local existingStr = redis.call('HGET', depositsKey, bidderId)
        local existing = 0
        if existingStr and existingStr ~= false then existing = tonumber(existingStr) end
        local needCharge = deposit - existing
        
        -- 추가 차감 필요하면 잔액 체크 후 차감
        if needCharge > 0 then
          local balStr = redis.call('GET', balKey)
          if not balStr then
            return -3
          end
          local bal = tonumber(balStr)
          if bal < needCharge then
            return -2
          end
          redis.call('SET', balKey, bal - needCharge)
          redis.call('INCR', verKey)
        end
        
        -- deposits에 최종 deposit 기록
        redis.call('HSET', depositsKey, bidderId, tostring(deposit))
        
        -- 이전 최고입찰자 deposit 즉시 환불
        if curBidder ~= '' then
          local prevDepStr = redis.call('HGET', depositsKey, curBidder)
          if prevDepStr and prevDepStr ~= false then
            local prevDep = tonumber(prevDepStr)
        
            local prevBalKey = 'auction:bizz:' .. curBidder
            local prevVerKey = 'auction:bizzver:' .. curBidder
        
            local prevBalStr = redis.call('GET', prevBalKey) or '0'
            redis.call('SET', prevBalKey, tonumber(prevBalStr) + prevDep)
            redis.call('INCR', prevVerKey)
        
            if balanceTtl and balanceTtl > 0 then
              redis.call('EXPIRE', prevBalKey, balanceTtl)
              redis.call('EXPIRE', prevVerKey, balanceTtl)
            end

            redis.call('HDEL', depositsKey, curBidder)
          end
        end
        
        -- 최고가 갱신
        redis.call('HSET', liveKey, 'maxBidPrice', tostring(newPrice))
        redis.call('HSET', liveKey, 'currentBidderId', bidderId)
        
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

    public Long updateMaxBidPriceAtomicWithDeposit(
            String redisKey,
            Long bidderId,
            Integer bidPrice,
            Long depositAmount,
            Long sessionTtlSeconds,
            Long balanceTtlSeconds
    ) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_BID_WITH_DEPOSIT_SCRIPT, Long.class);

        return redisTemplate.execute(
                script,
                Collections.singletonList(redisKey),
                bidderId.toString(),
                bidPrice.toString(),
                depositAmount.toString(),
                sessionTtlSeconds.toString(),
                balanceTtlSeconds.toString()
        );
    }

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
