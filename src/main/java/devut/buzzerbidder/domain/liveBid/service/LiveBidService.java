package devut.buzzerbidder.domain.liveBid.service;

import devut.buzzerbidder.domain.liveBid.dto.BidAtomicResult;
import devut.buzzerbidder.domain.liveBid.dto.LiveBidEvent;
import devut.buzzerbidder.domain.liveBid.dto.request.LiveBidRequest;
import devut.buzzerbidder.domain.liveBid.dto.response.LiveBidResponse;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.wallet.enums.WalletTransactionType;
import devut.buzzerbidder.domain.wallet.service.WalletHistoryService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveBidService {

    private final LiveItemRepository liveItemRepository;
    private final LiveBidRedisService liveBidRedisService;
    private final LiveBidWebSocketService liveBidWebSocketService;
    private final WalletHistoryService walletHistoryService;

    private static final String REDIS_KEY_PREFIX = "liveItem:";
    private static final String BID_TOPIC = "live-bid-events";

    public LiveBidResponse bid(LiveBidRequest request, User bidder) {
        // DB 조회 및 기본 검증
        LiveItem liveItem = liveItemRepository.findById(request.liveItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVEITEM_NOT_FOUND));

        String redisKey = REDIS_KEY_PREFIX + request.liveItemId();

        // 입력값 검증
        validateBidRequest(liveItem, bidder, redisKey);

        long depositAmount = (long) Math.ceil(request.bidPrice() * 0.2);

        long sessionTtlSeconds = 35L;
        long balanceTtlSeconds = 600L;

        // redis 입찰가 갱신 시도
        BidAtomicResult result = liveBidRedisService.updateMaxBidPriceAtomicWithDeposit(
                redisKey,
                request.liveItemId(),
                bidder.getId(),
                request.bidPrice(),
                depositAmount,
                sessionTtlSeconds,
                balanceTtlSeconds
        );

        // 입찰 시도 결과에 따른 분기 처리
        return handleBidResult(result, request, bidder, liveItem, redisKey, depositAmount);
    }

    /**
     * 입찰 전 필수 비즈니스 로직 검증
     */
    private void validateBidRequest(LiveItem liveItem, User bidder, String redisKey) {
        // 판매자 본인 입찰 불가 검증
        if(liveItem.getSellerUserId().equals(bidder.getId())) {
            throw new BusinessException(ErrorCode.LIVEBID_CANNOT_BID_OWN_ITEM);
        }

        // 경매 진행 중인 상품에만 입찰 가능
        if(liveItem.getAuctionStatus() != LiveItem.AuctionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.LIVEBID_NOT_IN_PROGRESS);
        }

    }

    /**
     * 입찰 결과 분기 처리
     * @return result 값이 1인 경우 입찰 성공, -1 또는 0인 경우 입찰 실패
     */
    private LiveBidResponse handleBidResult(
            BidAtomicResult result,
            LiveBidRequest request,
            User bidder,
            LiveItem liveItem,
            String redisKey,
            long depositAmount
    ) {
        if (result == null) {
            throw new BusinessException(ErrorCode.UNEXPECTED_REDIS_SCRIPT_RETURN);
        }

        long code = result.code();

        if (code == 1L) {
            Long balanceBefore = result.balanceBefore();
            Long balanceAfter = result.balanceAfter();
            if (balanceBefore == null || balanceAfter == null) {
                throw new BusinessException(ErrorCode.UNEXPECTED_REDIS_SCRIPT_RETURN);
            }

            processSuccessfulBid(request, bidder, liveItem.getSellerUserId());

            walletHistoryService.recordWalletHistory(
                    bidder,
                    depositAmount,
                    WalletTransactionType.BID,
                    balanceBefore,
                    balanceAfter
            );

            liveItem.setCurrentPrice(Long.valueOf(request.bidPrice()));
            liveItemRepository.save(liveItem);

            return new LiveBidResponse(true, "입찰 성공.", request.bidPrice());
        }

        if (code == -1L) {
            // 본인이 이미 최고입찰자인 경우 입찰 실패
            throw new BusinessException(ErrorCode.LIVEBID_ALREADY_HIGHEST_BIDDER);
        }

        if (code == -2L) {
            throw new BusinessException(ErrorCode.BIZZ_INSUFFICIENT_BALANCE);
        }

        if (code == -3L) {
            throw new BusinessException(ErrorCode.AUCTION_SESSION_EXPIRED);
        }

        if (code == -4L) {
            throw new BusinessException(ErrorCode.AUCTION_ENDED);
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

        liveBidRedisService.saveBidLogToStream(event);

        log.info("라이브 입찰 성공. Item: {} Price: {}", request.liveItemId(), request.bidPrice());

        // 웹소켓을 통해 클라이언트에게 최고가 갱신 브로드캐스트
        // destination: "/receive/auction/{auctionId}"
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
