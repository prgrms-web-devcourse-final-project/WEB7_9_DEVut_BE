package devut.buzzerbidder.domain.liveBid.service;

import io.micrometer.core.annotation.Timed;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor()
public class LiveBidRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // 종료 감지용 ZSET 키
    private static final String ENDING_ZSET_KEY = "auction:live:ending";

    // 시작 감지용 ZSET 키
    private static final String STARTING_ZSET_KEY = "auction:live:starting";

    /** 경매 종료 시각 등록/갱신 (score=endTimeMs, member=liveItemId) */
    public void upsertEndingZset(Long liveItemId, long endTimeMs) {
        redisTemplate.opsForZSet().add(ENDING_ZSET_KEY, liveItemId.toString(), (double) endTimeMs);
    }

    /** 경매 종료 감지용 ZSET key getter */
    public String getEndingZsetKey() {
        return ENDING_ZSET_KEY;
    }

    /** 경매 시작 시각 등록/갱신 (score=startTimeMs, member=liveItemId) */
    public void upsertStartingZset(Long liveItemId, long startTimeMs) {
        redisTemplate.opsForZSet().add(STARTING_ZSET_KEY, liveItemId.toString(), (double) startTimeMs);
    }

    /** 경매 시작 감지용 ZSET key getter */
    public String getStartingZsetKey() {
        return STARTING_ZSET_KEY;
    }

    // Redis HASH에 데이터 저장
    public void setHash(String key, Map<String, String> data) {
        redisTemplate.opsForHash().putAll(key, data);
    }

    // Redis HASH에서 필드 값 조회
    public String getHashField(String key, String field) {
        Object result = redisTemplate.opsForHash().get(key, field);
        return result != null ? result.toString() : null;
    }
    // Redis 서버 시간(ms) 조회: JVM 시간 차이로 인한 조기/지연 종료 방지용
    public long getRedisNowMs() {
        final String LUA_TIME_MS = """
            local t = redis.call('TIME')
            return tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
        """;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_TIME_MS, Long.class);
        Long nowMs = redisTemplate.execute(script, Collections.emptyList());
        if (nowMs == null) {
            throw new IllegalStateException("Redis TIME 조회 결과가 null입니다.");
        }
        return nowMs;
    }
    // 현재 최고가보다 높을 경우에만 갱신 (원자성 보장)
    // KEYS[1]: redisKey
    // KEYS[2] 가격 필터용 ZSET 키
    // KEYS[3] 입찰 존재 SET 키
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
        local endingZsetKey = KEYS[2]
        local bidZKey = KEYS[2]
        local hasBidKey = KEYS[3]
        local depositsKey = liveKey .. ':deposits'
        
        local liveItemId = string.match(liveKey, '^liveItem:(.+)$')
        
        local newBidderId = tostring(ARGV[1])
        local newPrice = tonumber(ARGV[2])
        local deposit = tonumber(ARGV[3])
        local sessionTtl = tonumber(ARGV[4])
        local balanceTtl = tonumber(ARGV[5])
        local liveItemId = tostring(ARGV[6])
        
        -- Redis TIME 기반 nowMs 생성 (서버 시간 차이 방지)
        local t = redis.call('TIME')             -- {sec, usec}
        local nowMs = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
        
        -- 추가: endTime 확인 (없으면 초기화 안 된 것)
        local endTimeStr = redis.call('HGET', liveKey, 'endTime')
        if (not endTimeStr) or (endTimeStr == false) then
          return -3
        end
        local endTime = tonumber(endTimeStr)
                        
        -- 추가: 이미 종료 시간이 지났으면 입찰 거절
        if nowMs >= endTime then
          return -4
        end
                        
        -- wallet keys
        local sesKey = 'auction:session:' .. newBidderId
        local balKey = 'auction:bizz:' .. newBidderId
        local verKey = 'auction:bizzver:' .. newBidderId
        
        -- 세션 없으면 실패
        if redis.call('EXISTS', sesKey) == 0 then
          return -3
        end
        
        -- verKey 없으면 실패 (TTL 꼬임/상태 이상 방지)
        if redis.call('EXISTS', verKey) == 0 then
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
        
        -- 환불에 필요한 값은 쓰기(차감/HSET) 전에 미리 읽고 검증(부분반영 방지)
                    local prevDep = nil
                    local prevBalKey = nil
                    local prevVerKey = nil
                    local prevBal = nil
            
                    if prevBidder ~= '' then
                      local prevDepStr = redis.call('HGET', depositsKey, prevBidder)
                      if prevDepStr and prevDepStr ~= false then
                        prevDep = tonumber(prevDepStr)
                        prevBalKey = 'auction:bizz:' .. prevBidder
                        prevVerKey = 'auction:bizzver:' .. prevBidder
            
                        local prevBalStr = redis.call('GET', prevBalKey)
                        if (not prevBalStr) or (redis.call('EXISTS', prevVerKey) == 0) then
                          return -3
                        end
                        prevBal = tonumber(prevBalStr)
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
        
        -- 이전 최고입찰자 환불: 위에서 미리 읽어둔 값(prevDep/prevBal)만 사용
       if prevDep ~= nil then
         redis.call('SET', prevBalKey, prevBal + prevDep)
         redis.call('INCR', prevVerKey)

         if balanceTtl and balanceTtl > 0 then
           redis.call('EXPIRE', prevBalKey, balanceTtl)
           redis.call('EXPIRE', prevVerKey, balanceTtl)
         end

         redis.call('HDEL', depositsKey, prevBidder)
       end

        
        -- 최고가 갱신
        redis.call('HSET', liveKey, 'maxBidPrice', tostring(newPrice))
        redis.call('HSET', liveKey, 'currentBidderId', newBidderId)
        
        -- 10초 미만 입찰 시 10초로 초기화
        local minEnd = nowMs + 10000
        if endTime < minEnd then
          endTime = minEnd
          redis.call('HSET', liveKey, 'endTime', tostring(endTime))
        end
        
        -- 종료 감지 ZSET score 갱신
        redis.call('ZADD', endingZsetKey, endTime, liveItemId)
                        
        -- 가격 필터링용 bidZKey, hasBidKey 갱신
        if bidZKey and bidZKey ~= '' then
            redis.call('ZADD', bidZKey, newPrice, liveItemId)
        end
        
        if hasBidKey and hasBidKey ~= '' then
            redis.call('SADD', hasBidKey, liveItemId)
        end
        
        -- TTL 연장
        if sessionTtl and sessionTtl > 0 then
          redis.call('EXPIRE', sesKey, sessionTtl)
        end
        
        if balanceTtl and balanceTtl > 0 then
          redis.call('EXPIRE', balKey, balanceTtl)
          redis.call('EXPIRE', verKey, balanceTtl)
          redis.call('EXPIRE', depositsKey, balanceTtl)
          redis.call('EXPIRE', liveKey, balanceTtl)
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
            Long liveItemId,
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
                    List.of(redisKey, ENDING_ZSET_KEY),
                    Arrays.asList(
                        redisKey,                       // KEYS[1] 기존 LiveItem 키
                        "liveItems:currentPrice",       // KEYS[2] 가격 필터용 ZSET 키
                        "liveItems:hasBid"              // KEYS[3] 입찰 존재 SET 키
                    ),
                    bidderId.toString(),
                    bidPrice.toString(),
                    depositAmount.toString(),
                    sessionTtlSeconds.toString(),
                    balanceTtlSeconds.toString(),
                    liveItemId.toString()
            );
        } catch (DataAccessException e) {
            // 레디스 연결/타임아웃/스크립트 실행 실패 등은 여기로 옴
            throw new IllegalStateException("Redis LUA 실행 실패. redisKey=" + redisKey + ", bidderId=" + bidderId, e);
        }
    }

    /**
     * ending ZSET에서 (score <= nowMs) 인 liveItemId들을 limit 만큼 꺼냄
     * 여러 서버/스레드가 돌아도 같은 itemId를 중복 처리하지 않게 하려고 ZREM까지 같이 함
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<Long> popDueEndingItems(long nowMs, int limit) {

        // ZSET에서 due를 뽑고, 뽑은 애들을 ZREM으로 제거하는 Lua
        final String LUA_POP_DUE = """
        local zkey = KEYS[1]
        local now = tonumber(ARGV[1])
        local lim = tonumber(ARGV[2])

        local items = redis.call('ZRANGEBYSCORE', zkey, '-inf', now, 'LIMIT', 0, lim)
        if #items == 0 then
          return {}
        end

        -- ZREM은 unpack이 필요 (items가 1개여도 안전)
        redis.call('ZREM', zkey, unpack(items))
        return items
    """;

        DefaultRedisScript<List> script = new DefaultRedisScript<>(LUA_POP_DUE, List.class);

        List<?> raw = redisTemplate.execute(
                script,
                List.of(ENDING_ZSET_KEY),
                String.valueOf(nowMs),
                String.valueOf(limit)
        );

        if (raw == null || raw.isEmpty()) return List.of();

        return raw.stream()
                .map(String::valueOf)
                .map(Long::parseLong)
                .toList();
    }

    /**
     * starting ZSET에서 (score <= nowMs) 인 liveItemId들을 limit 만큼 꺼냄
     * 여러 서버/스레드가 돌아도 같은 itemId를 중복 처리하지 않게 하려고 ZREM까지 같이 함
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<Long> popDueStartingItems(long nowMs, int limit) {

        // ZSET에서 due를 뽑고, 뽑은 애들을 ZREM으로 제거하는 Lua
        final String LUA_POP_DUE = """
        local zkey = KEYS[1]
        local now = tonumber(ARGV[1])
        local lim = tonumber(ARGV[2])

        local items = redis.call('ZRANGEBYSCORE', zkey, '-inf', now, 'LIMIT', 0, lim)
        if #items == 0 then
          return {}
        end

        -- ZREM은 unpack이 필요 (items가 1개여도 안전)
        redis.call('ZREM', zkey, unpack(items))
        return items
    """;

        DefaultRedisScript<List> script = new DefaultRedisScript<>(LUA_POP_DUE, List.class);

        List<?> raw = redisTemplate.execute(
                script,
                List.of(STARTING_ZSET_KEY),
                String.valueOf(nowMs),
                String.valueOf(limit)
        );

        if (raw == null || raw.isEmpty()) return List.of();

        return raw.stream()
                .map(String::valueOf)
                .map(Long::parseLong)
                .toList();
    }

    public void deleteLiveItemRedisKeys(Long liveItemId) {
        String liveKey = "liveItem:" + liveItemId;
        String depositsKey = liveKey + ":deposits";

        redisTemplate.delete(List.of(liveKey, depositsKey));

        // 멱등 정리
        redisTemplate.opsForZSet().remove(getEndingZsetKey(), liveItemId.toString());
        redisTemplate.opsForZSet().remove(getStartingZsetKey(), liveItemId.toString());
    public List<Long> zRangeByScoreAsLong(String zsetKey, long min, long max) {
        Set<String> members = redisTemplate.opsForZSet().rangeByScore(zsetKey, min, max);
        if (members == null || members.isEmpty()) return List.of();
        return members.stream().map(Long::valueOf).toList();
    }

    public List<Boolean> sIsMemberBatch(String setKey, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        @SuppressWarnings("unchecked")
        List<Object> raw = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            byte[] key = redisTemplate.getStringSerializer().serialize(setKey);
            for (Long id : ids) {
                byte[] member = redisTemplate.getStringSerializer().serialize(id.toString());
                connection.sIsMember(key, member);
            }
            return null;
        });

        // sIsMember는 Boolean(또는 0/1)로 돌아올 수 있음
        return raw.stream().map(v -> {
            if (v instanceof Boolean b) return b;
            if (v instanceof Long l) return l == 1L;
            return false;
        }).toList();
    }

}
