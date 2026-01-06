package devut.buzzerbidder.domain.liveBid.service;

import java.time.Duration;

import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HeartbeatService {

    private final StringRedisTemplate redis;

    private static final Duration SESSION_TTL = Duration.ofSeconds(35);
    private static final Duration BALANCE_TTL = Duration.ofMinutes(10);

    public void heartbeat(Long userId) {
        String sesKey = "auction:session:" + userId;
        String balKey = "auction:bizz:" + userId;
        String verKey = "auction:bizzver:" + userId;

        Boolean exists = redis.expire(sesKey, SESSION_TTL);
        if (!Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCode.AUCTION_SESSION_EXPIRED);
        }

        redis.expire(balKey, BALANCE_TTL);
        redis.expire(verKey, BALANCE_TTL);
    }
}
