package devut.buzzerbidder.domain.deal.service;

import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.DelayedDealRepository;
import devut.buzzerbidder.domain.delayedbid.entity.DelayedBidLog;
import devut.buzzerbidder.domain.delayedbid.repository.DelayedBidRepository;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.AuctionStatus;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.deliveryTracking.dto.response.DeliveryTrackingResponse;
import devut.buzzerbidder.domain.deliveryTracking.service.DeliveryTrackingService;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DelayedDealService {

    private final DelayedDealRepository delayedDealRepository;
    private final DelayedItemRepository delayedItemRepository;
    private final DelayedBidRepository delayedBidRepository;
    private final UserRepository userRepository;
    private final DeliveryTrackingService deliveryTrackingService;

    public DelayedDeal findByIdOrThrow(Long dealId) {
        return delayedDealRepository.findById(dealId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));
    }

    // 경매 종료 후 낙찰 처리
    @Transactional
    public DelayedDeal createDealFromAuction(Long delayedItemId) {
        DelayedItem item = delayedItemRepository.findById(delayedItemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        // 이미 Deal이 생성되었는지 확인
        Optional<DelayedDeal> existingDeal = delayedDealRepository.findByItem(item);
        if (existingDeal.isPresent()) {
            return existingDeal.get();
        }

        // 경매 종료 확인
        if (!item.isAuctionEnded()) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_ENDED);
        }

        // 최고가 입찰자 조회
        DelayedBidLog highestBid = delayedBidRepository
            .findTopByDelayedItemIdOrderByBidAmountDesc(item.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NO_BID_EXISTS));

        User buyer = userRepository.findById(highestBid.getBidderUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // DelayedDeal 생성
        DelayedDeal deal = DelayedDeal.builder()
            .item(item)
            .buyer(buyer)
            .winningPrice(highestBid.getBidAmount())
            .status(DealStatus.PENDING)
            .build();

        return delayedDealRepository.save(deal);
    }

    // 배송 정보 입력
    @Transactional
    public void patchDeliveryInfo(User currentUser, Long dealId, String carrierCode, String trackingNumber) {
        DelayedDeal deal = findByIdOrThrow(dealId);

        // 판매자 권한 체크
        if (!deal.getItem().getSellerUserId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        deal.updateDeliveryInfo(carrierCode, trackingNumber);
        deal.updateStatus(DealStatus.SHIPPING);
    }

    // 배공 조회
    public DeliveryTrackingResponse track(User currentUser, Long dealId) {
        DelayedDeal deal = findByIdOrThrow(dealId);

        // 판매자 또는 구매자 권한 체크
        boolean isSeller = deal.getItem().getSellerUserId().equals(currentUser.getId());
        boolean isBuyer = deal.getBuyer().getId().equals(currentUser.getId());
        if (!isSeller && !isBuyer) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        String carrierCode = deal.getCarrier() != null ? deal.getCarrier().getCode() : null;
        String trackingNumber = deal.getTrackingNumber();

        if (carrierCode == null || carrierCode.isBlank() || trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }

        return deliveryTrackingService.track(carrierCode, trackingNumber);
    }

    // 구매 확정
    @Transactional
    public void confirmPurchase(User currentUser, Long dealId) {
        DelayedDeal deal = findByIdOrThrow(dealId);

        // 구매자 권한 체크
        if (!deal.getBuyer().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 상태 체크 - PAID나 SHIPPING 상태여야 구매 확정 가능
        if (deal.getStatus() != DealStatus.PAID && deal.getStatus() != DealStatus.SHIPPING) {
            throw new BusinessException(ErrorCode.DEAL_INVALID_STATUS);
        }

        // Deal 상태 업데이트
        deal.updateStatus(DealStatus.COMPLETED);

        // DelayedItem 상태 업데이트
        deal.getItem().changeAuctionStatus(
            AuctionStatus.PURCHASE_CONFIRMED
        );
    }


}
