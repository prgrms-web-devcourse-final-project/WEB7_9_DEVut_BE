package devut.buzzerbidder.domain.liveBid.service;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
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

    private static final String REDIS_KEY_PREFIX = "liveItem:";

    @Transactional
    public LiveBidResponse bid(LiveBidRequest request, User bidder) {

        LiveItem liveItem = liveItemRepository.findById(request.liveItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVEITEM_NOT_FOUND));

        Long sellerId = liveItem.getSellerUserId();
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        AuctionRoom auctionRoom = liveItem.getAuctionRoom();

        String redisKey = REDIS_KEY_PREFIX + request.liveItemId();
        String bidPriceStr = String.valueOf(request.bidPrice());
        String bidderIdStr = String.valueOf(bidder.getId());

        Long result = liveBidRedisService.updateMaxBidPriceAtomic(
                redisKey,
                bidPriceStr,
                bidderIdStr
        );

        if (result == 1) {
            // redis 최고가 갱신 성공. 입찰 로그 DB에 저장
            // TODO: 후에 kafka 도입하여 DB 부하 최소화
            LiveBidLog logEntity = LiveBidLog.builder()
                    .bidder(bidder)
                    .liveItem(liveItem)
                    .seller(seller)
                    .auctionRoom(auctionRoom)
                    .bidPrice(request.bidPrice())
                    .build();
            liveBidRepository.save(logEntity);
            log.info("라이브 입찰 성공. Item: {} Price: {}", request.liveItemId(), request.bidPrice());

            // 웹소켓을 통해 클라이언트에게 최고가 갱신 브로드캐스트
            // destination: "/receive/auction/{auctionRoomId}"
            liveBidWebSocketService.broadcastNewBid(
                    request.auctionId(),
                    request.liveItemId(),
                    request.bidPrice(),
                    bidder.getId()
            );

            return new LiveBidResponse(true, "입찰 성공.", request.bidPrice());
        } else { // 갱신 실패
            // 현재 최고가를 Redis에서 다시 읽어와서 반환
            String currentMaxPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");
            int currentMaxPrice = Integer.parseInt(currentMaxPriceStr);

            return new LiveBidResponse(
                    false,
                    "입찰 실패. 현재 최고가보다 낮은 가격입니다. 현재가: " + currentMaxPrice,
                    currentMaxPrice
            );
        }

    }

    // 경매 시작 후 경매 정보 초기화
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
}
