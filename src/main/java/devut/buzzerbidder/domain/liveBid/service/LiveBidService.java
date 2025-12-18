package devut.buzzerbidder.domain.liveBid.service;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.liveBid.dto.LiveBidEvent;
import devut.buzzerbidder.domain.liveBid.dto.request.LiveBidRequest;
import devut.buzzerbidder.domain.liveBid.dto.response.LiveBidResponse;
import devut.buzzerbidder.domain.liveBid.entity.LiveBidLog;
import devut.buzzerbidder.domain.liveBid.repository.LiveBidRepository;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveBidService {

    private final UserRepository userRepository;
    private final LiveBidRepository liveBidRepository;
    private final LiveItemRepository liveItemRepository;
    private final LiveBidRedisService liveBidRedisService;
    private final LiveBidWebSocketService liveBidWebSocketService;
    private final KafkaTemplate<String, LiveBidEvent> kafkaTemplate;

    private static final String REDIS_KEY_PREFIX = "liveItem:";
    private static final String BID_TOPIC = "live-bid-events";

    public LiveBidResponse bid(LiveBidRequest request, User bidder) {
        LiveItem liveItem = liveItemRepository.findById(request.liveItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVEITEM_NOT_FOUND));

        validateBidRequest(liveItem, bidder);

        // redis 입찰가 갱신 시도
        String redisKey = REDIS_KEY_PREFIX + request.liveItemId();
        Long result = liveBidRedisService.updateMaxBidPriceAtomic(
                redisKey,
                String.valueOf(request.bidPrice()),
                String.valueOf(bidder.getId())
        );

        // 입찰 시도 결과에 따른 분기 처리
        return handleBidResult(result, request, bidder, liveItem.getSellerUserId(), redisKey);
    }

    /**
     * 실시간 입찰 정보 redis에 최초 캐시
     * @param liveItemId 라이브 경매품 ID
     */
    public void initLiveItem(Long liveItemId) {
        LiveItem liveItem = liveItemRepository.findById(liveItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVEITEM_NOT_FOUND));

        String redisKey = REDIS_KEY_PREFIX + liveItemId;

        Map<String, String> initialData = new HashMap<>();

        // 최초 입찰가는 시작가, 최초 입찰자는 없음.
        initialData.put("maxBidPrice", String.valueOf(liveItem.getInitPrice()));
        initialData.put("currentBidderId", "");

        liveBidRedisService.setHash(redisKey, initialData);
    }

    /**
     * 입찰 전 필수 비즈니스 로직 검증
     */
    private void validateBidRequest(LiveItem liveItem, User bidder) {
        // 판매자 본인 입찰 불가 검증
        if(liveItem.getSellerUserId().equals(bidder.getId())) {
            throw new BusinessException(ErrorCode.LIVEBID_CANNOT_BID_OWN_ITEM);
        }

        // 경매 진행 중인 상품에만 입찰 가능
        if(liveItem.getAuctionStatus() != LiveItem.AuctionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.LIVEBID_NOT_IN_PROGRESS);
        }

        // TODO: 지갑 잔고 검증
    }

    /**
     * 입찰 결과 분기 처리
     * @return result 값이 1인 경우 입찰 성공, -1 또는 0인 경우 입찰 실패
     */
    private LiveBidResponse handleBidResult(Long result, LiveBidRequest request, User bidder, Long sellerId, String redisKey) {
        if (result == 1) {
            processSuccessfulBid(request, bidder, sellerId);
            return new LiveBidResponse(true, "입찰 성공.", request.bidPrice());
        }

        if (result == -1) {
            // 본인이 이미 최고입찰자인 경우 입찰 실패
            throw new BusinessException(ErrorCode.LIVEBID_ALREADY_HIGHEST_BIDDER);
        }

        // result가 0인 경우 (입찰가 낮음)
        return handleFailedBid(redisKey);
    }

    private void processSuccessfulBid(LiveBidRequest request, User bidder, Long sellerId) {
        // redis 최고가 갱신 성공.
        LiveBidEvent event = new LiveBidEvent(
                request.auctionId(),
                request.liveItemId(),
                bidder.getId(),
                sellerId,
                request.bidPrice()
        );

        // itemId를 키로 하여 각 경매품의 입찰 순서 보장. kafka에 이벤트 발행
        kafkaTemplate.send(BID_TOPIC, String.valueOf(request.liveItemId()), event);
        // TODO: Kafka 발행 실패 시 재시도 로직 구현

        log.info("라이브 입찰 성공. Item: {} Price: {}", request.liveItemId(), request.bidPrice());

        // 웹소켓을 통해 클라이언트에게 최고가 갱신 브로드캐스트
        // destination: "/receive/auction/{auctionRoomId}"
        liveBidWebSocketService.broadcastNewBid(
                request.auctionId(), request.liveItemId(), request.bidPrice(), bidder.getId()
        );
    }

    private LiveBidResponse handleFailedBid(String redisKey) {
        // 현재 최고가를 Redis에서 다시 읽어와서 반환
        String currentMaxPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");
        int currentMaxPrice = (currentMaxPriceStr != null) ? Integer.parseInt(currentMaxPriceStr) : 0;

        return new LiveBidResponse(
                false,
                "입찰 실패. 현재 최고가보다 낮은 가격입니다. 현재가: " + currentMaxPrice,
                currentMaxPrice
        );
    }
}
