package devut.buzzerbidder.domain.liveBid.service;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.liveBid.entity.LiveBidLog;
import devut.buzzerbidder.domain.liveBid.repository.LiveBidLogRepository;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 입찰 로그 DB 저장을 담당하는 서비스
 * 별도 Bean으로 분리하여 @Transactional, @Retryable 프록시가 정상 동작하도록 함
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveBidLogPersistService {

    private final LiveBidLogRepository liveBidLogRepository;
    private final EntityManager entityManager;

    /**
     * 실제 DB 저장 로직 (재시도 대상)
     * 예외 발생 시: 1초 대기 후 재시도, 최대 3회 시도
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    @Transactional
    public void processAndSave(MapRecord<String, String, String> message) {
        Map<String, String> data = message.getValue();

        Long auctionId = Long.parseLong(data.get("auctionId"));
        Long liveItemId = Long.parseLong(data.get("liveItemId"));
        Long bidderId = Long.parseLong(data.get("bidderId"));
        Long sellerId = Long.parseLong(data.get("sellerId"));
        int bidPrice = Integer.parseInt(data.get("bidPrice"));

        String occurredAtStr = data.get("occurredAt");
        LocalDateTime occurredAt;

        if (occurredAtStr != null) {
            long timestamp = Long.parseLong(occurredAtStr);
            occurredAt = Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } else {
            occurredAt = LocalDateTime.now();
        }

        User bidderProxy = entityManager.getReference(User.class, bidderId);
        User sellerProxy = entityManager.getReference(User.class, sellerId);
        LiveItem itemProxy = entityManager.getReference(LiveItem.class, liveItemId);
        AuctionRoom roomProxy = entityManager.getReference(AuctionRoom.class, auctionId);

        LiveBidLog bidLog = LiveBidLog.builder()
                .bidder(bidderProxy)
                .seller(sellerProxy)
                .liveItem(itemProxy)
                .auctionRoom(roomProxy)
                .bidPrice(bidPrice)
                .bidTime(occurredAt)
                .build();

        liveBidLogRepository.save(bidLog);
        log.debug("입찰 로그 DB 저장 성공: item={}, price={}", liveItemId, bidPrice);
    }
}

