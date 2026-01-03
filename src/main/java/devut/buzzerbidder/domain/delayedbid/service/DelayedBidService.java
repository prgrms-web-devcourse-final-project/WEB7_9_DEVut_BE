package devut.buzzerbidder.domain.delayedbid.service;

import devut.buzzerbidder.domain.deal.service.DelayedDealService;
import devut.buzzerbidder.domain.delayedbid.dto.DelayedBidListResponse;
import devut.buzzerbidder.domain.delayedbid.dto.DelayedBidRequest;
import devut.buzzerbidder.domain.delayedbid.dto.DelayedBidResponse;
import devut.buzzerbidder.domain.delayedbid.entity.DelayedBidLog;
import devut.buzzerbidder.domain.delayedbid.event.DelayedBidOutbidEvent;
import devut.buzzerbidder.domain.delayedbid.event.DelayedBuyNowEvent;
import devut.buzzerbidder.domain.delayedbid.event.DelayedFirstBidEvent;
import devut.buzzerbidder.domain.delayedbid.repository.DelayedBidRepository;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.AuctionStatus;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DelayedBidService {

    private final DelayedBidRepository delayedBidRepository;
    private final DelayedItemRepository delayedItemRepository;
    private final DelayedDealService delayedDealService;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DelayedBidResponse placeBid(Long delayedItemId, DelayedBidRequest request, User user) {

        // 1. 경매품 조회 (비관적 락 - 동시성 제어)
        DelayedItem delayedItem = delayedItemRepository.findByIdWithLock(delayedItemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        // 2. 본인 물품 입찰 불가
        if (delayedItem.getSellerUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_BID_OWN_ITEM);
        }

        // 3. 경매 상태 및 종료 시간 확인
        if (!delayedItem.canBid()) {
            throw new BusinessException(ErrorCode.AUCTION_ALREADY_ENDED);
        }

        // 4. 입찰 금액 검증 (현재가보다 높아야 함)
        if (request.bidPrice() <= delayedItem.getCurrentPrice()) {
            throw new BusinessException(ErrorCode.BID_PRICE_TOO_LOW);
        }

        // 4-1. 즉시 구매가 이상 입찰시 즉시 구매 처리
        if (delayedItem.hasBuyNowPrice()
            && request.bidPrice() >= delayedItem.getBuyNowPrice()) {

            return buyNow(delayedItemId, user);
        }

        // 5. 코인 잔액 확인
        if (!walletService.hasEnoughBizz(user, request.bidPrice())) {
            throw new BusinessException(ErrorCode.BIZZ_INSUFFICIENT_BALANCE);
        }

        // 6. 이전 최고가 입찰 확인 및 환불
        Long previousBidderUserId = null;
        Optional<DelayedBidLog> previousBidOpt = delayedBidRepository
            .findTopByDelayedItemIdOrderByBidAmountDesc(delayedItemId);

        if (previousBidOpt.isPresent()) {
            DelayedBidLog previousBid = previousBidOpt.get();
            previousBidderUserId = previousBid.getBidderUserId();

            // 본인이 이미 최고가 입찰자인 경우 재입찰 불가
            if (previousBid.getBidderUserId().equals(user.getId())) {
                throw new BusinessException(ErrorCode.ALREADY_HIGHEST_BIDDER);
            }

            // 이전 최고가 입찰자에게 환불
            User previousBidder = userRepository.findById(previousBid.getBidderUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            walletService.refundBidBizz(previousBidder, previousBid.getBidAmount());
        }
        // 코인 차감
        walletService.bidBizz(user, request.bidPrice());

        // 7. 입찰 로그 저장
        DelayedBidLog bidLog = DelayedBidLog.builder()
            .delayedItem(delayedItem)
            .bidderUserId(user.getId())
            .bidAmount(request.bidPrice())
            .bidTime(LocalDateTime.now())
            .build();

        delayedBidRepository.save(bidLog);

        // 8. 첫 입찰시 상태 변경
        if (delayedItem.getAuctionStatus() == AuctionStatus.BEFORE_BIDDING) {
            delayedItem.changeAuctionStatus(AuctionStatus.IN_PROGRESS);

            eventPublisher.publishEvent(
                new DelayedFirstBidEvent(
                    delayedItem.getId(),
                    delayedItem.getName(),
                    delayedItem.getSellerUserId(),
                    user.getId(),
                    request.bidPrice()
                )
            );
        }

        // 9. 경매품의 현재가 업데이트
        delayedItem.updateCurrentPrice(request.bidPrice());
        delayedItemRepository.save(delayedItem);

        if (previousBidderUserId != null) {
            eventPublisher.publishEvent(
                new DelayedBidOutbidEvent(
                    delayedItem.getId(),
                    delayedItem.getName(),
                    previousBidderUserId,
                    user.getId(),
                    request.bidPrice()
                )
            );
        }

        // 10. 응답 생성
        return new DelayedBidResponse(
            bidLog.getId(),
            delayedItem.getId(),
            user.getNickname(),
            bidLog.getBidAmount(),
            bidLog.getCreateDate()
        );
    }

    @Transactional(readOnly = true)
    public DelayedBidListResponse getBidHistory(Long delayedItemId, Pageable pageable) {

        DelayedItem delayedItem = delayedItemRepository.findById(delayedItemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        Page<DelayedBidLog> page = delayedBidRepository
            .findByDelayedItemOrderByBidTimeDesc(delayedItem, pageable);

        List<DelayedBidResponse> bidList = page.getContent().stream()
            .map(bidLog -> {
                User bidder = userRepository.findById(bidLog.getBidderUserId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

                return new DelayedBidResponse(
                    bidLog.getId(),
                    delayedItem.getId(),
                    bidder.getNickname(),
                    bidLog.getBidAmount(),
                    bidLog.getCreateDate()
                );
            })
            .toList();

        return new DelayedBidListResponse(bidList, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public DelayedBidListResponse getMyBids(User user, Pageable pageable) {

        Page<DelayedBidLog> page = delayedBidRepository
            .findByBidderUserIdOrderByBidTimeDesc(user.getId(), pageable);

        List<DelayedBidResponse> bidList = page.getContent().stream()
            .map(bidLog -> new DelayedBidResponse(
                bidLog.getId(),
                bidLog.getDelayedItem().getId(),
                user.getNickname(),
                bidLog.getBidAmount(),
                bidLog.getCreateDate()
            ))
            .toList();

        return new DelayedBidListResponse(bidList, page.getTotalElements());
    }

    @Transactional
    public DelayedBidResponse buyNow(Long itemId, User buyer) {

        // 1. 비관적 락으로 경매 조회
        DelayedItem item = delayedItemRepository.findByIdWithLock(itemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        // 2. 즉시 구매가 설정 확인
        if (!item.hasBuyNowPrice()) {
            throw new BusinessException(ErrorCode.BUY_NOW_NOT_AVAILABLE);
        }

        // 3. 본인 물품 구매 불가
        if (item.getSellerUserId().equals(buyer.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_BID_OWN_ITEM);
        }

        // 4. 경매 진행 상태 확인 (상품 경매 상태 + 종료 시간 체크)
        if (!item.canBid()) {
            throw new BusinessException(ErrorCode.AUCTION_ALREADY_ENDED);
        }

        // 5. 구매자 코인 확인 및 차감
        if (!walletService.hasEnoughBizz(buyer, item.getBuyNowPrice())) {
            throw new BusinessException(ErrorCode.BIZZ_INSUFFICIENT_BALANCE);
        }
        walletService.bidBizz(buyer, item.getBuyNowPrice());

        // 6. 기존 최고가 입찰자에게 환불 (있는 경우)
        DelayedBidLog currentHighestBid = delayedBidRepository
            .findTopByDelayedItemIdOrderByBidAmountDesc(itemId)
            .orElse(null);

        if (currentHighestBid != null) {
            User previousBidder = userRepository.findById(currentHighestBid.getBidderUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            walletService.refundBidBizz(previousBidder, currentHighestBid.getBidAmount());
        }

        // 7. 즉시 구매 입찰 기록 생성
        DelayedBidLog buyNowBid = DelayedBidLog.builder()
            .delayedItem(item)
            .bidderUserId(buyer.getId())
            .bidAmount(item.getBuyNowPrice())
            .bidTime(LocalDateTime.now())
            .build();
        delayedBidRepository.save(buyNowBid);

        // 8. 현재가 업데이트
        item.updateCurrentPrice(item.getBuyNowPrice());

        // 9. 경매 즉시 종료 및 거래 생성
        item.changeAuctionStatus(AuctionStatus.IN_DEAL);
        delayedDealService.createDealFromAuction(itemId);

        eventPublisher.publishEvent(
            new DelayedBuyNowEvent(
                item.getId(),
                item.getName(),
                buyer.getId(),
                item.getSellerUserId(),
                currentHighestBid != null ? currentHighestBid.getBidderUserId() : null,
                item.getBuyNowPrice()
            )
        );

        return new DelayedBidResponse(
            buyNowBid.getId(),
            item.getId(),
            buyer.getNickname(),
            item.getBuyNowPrice(),
            buyNowBid.getCreateDate()
        );
    }
}
