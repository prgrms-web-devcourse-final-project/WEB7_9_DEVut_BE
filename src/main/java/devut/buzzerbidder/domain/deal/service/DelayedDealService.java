package devut.buzzerbidder.domain.deal.service;

import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.DelayedDealRepository;
import devut.buzzerbidder.domain.delayedbid.entity.DelayedBidLog;
import devut.buzzerbidder.domain.delayedbid.repository.DelayedBidRepository;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
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
            .findTopByDelayedItemOrderByBidAmountDesc(item)
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
    public void patchDeliveryInfo(Long currentUser, Long dealId, String carrierCode, String trackingNumber) {
        // TODO : 권한 체크 로직 추가 (currentUser가 판매자인지 확인)
        DelayedDeal deal = findByIdOrThrow(dealId);
        deal.updateDeliveryInfo(carrierCode, trackingNumber);
        deal.updateStatus(DealStatus.SHIPPING);
    }

    // 배공 조회
    public DeliveryTrackingResponse track(User currentUser, Long dealId) {
        // TODO: 권한 체크 로직 추가 (currentUser가 해당 deal에 접근할 수 있는지 확인)

        DelayedDeal deal = findByIdOrThrow(dealId);

        String carrierCode = deal.getCarrier() != null ? deal.getCarrier().getCode() : null;
        String trackingNumber = deal.getTrackingNumber();

        if (carrierCode == null || carrierCode.isBlank() || trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }

        return deliveryTrackingService.track(carrierCode, trackingNumber);
    }


}
