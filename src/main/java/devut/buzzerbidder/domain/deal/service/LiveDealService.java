package devut.buzzerbidder.domain.deal.service;

import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.AuctionType;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.event.ItemShippedEvent;
import devut.buzzerbidder.domain.deal.event.PaymentTimeoutEvent;
import devut.buzzerbidder.domain.deal.event.TransactionCompleteEvent;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.deliveryTracking.dto.response.DeliveryTrackingResponse;
import devut.buzzerbidder.domain.deliveryTracking.service.DeliveryTrackingService;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveDealService {

    private final LiveDealRepository liveDealRepository;
    private final LiveItemRepository liveItemRepository;
    private final DeliveryTrackingService deliveryTrackingService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public LiveDeal findByIdOrThrow(Long dealId) {
        return liveDealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));
    }

    // TODO: 라이브 딜 생성 시 기본 배송지 자동 설정 (나중에 구현)

    @Transactional
    public void createDeal(Long liveItemId, Long buyerId, Long winningPrice) {
        LiveItem item = liveItemRepository.findByIdWithLock(liveItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));
        User buyer = userService.findById(buyerId);

        // DelayedDeal 생성
        LiveDeal deal = LiveDeal.builder()
                .item(item)
                .buyer(buyer)
                .winningPrice(winningPrice)
                .status(DealStatus.PENDING)
                .build();

        liveDealRepository.save(deal);
    }

    @Transactional
    public void patchDeliveryInfo(User currentUser, Long dealId, String carrierCode, String trackingNumber) {
        LiveDeal liveDeal = findByIdOrThrow(dealId);

        // 판매자 권한 체크
        if (!liveDeal.getItem().getSellerUserId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        liveDeal.updateDeliveryInfo(carrierCode, trackingNumber);
        liveDeal.updateStatus(DealStatus.SHIPPING);
        eventPublisher.publishEvent(
            new ItemShippedEvent(
                liveDeal.getId(),
                liveDeal.getBuyer().getId(),
                liveDeal.getItem().getSellerUserId(),
                liveDeal.getItem().getId(),
                AuctionType.LIVE,
                liveDeal.getItem().getName(),
                liveDeal.getCarrier().getDisplayName(),
                liveDeal.getTrackingNumber()
            )
        );
    }

    public DeliveryTrackingResponse track(User currentUser, Long dealId) {
        // TODO: 권한 체크 로직 추가 (currentUser가 해당 deal에 접근할 수 있는지 확인)

        LiveDeal liveDeal = findByIdOrThrow(dealId);

        String carrierCode = liveDeal.getCarrier() != null ? liveDeal.getCarrier().getCode() : null;
        String trackingNumber = liveDeal.getTrackingNumber();

        if (carrierCode == null || carrierCode.isBlank() || trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }

        return deliveryTrackingService.track(carrierCode, trackingNumber);
    }

    // 거래 배송지 주소 수정
    @Transactional
    public void updateDeliveryAddress(User currentUser, Long dealId, String address, String addressDetail, String postalCode) {
        LiveDeal deal = findByIdOrThrow(dealId);

        // 구매자 권한 체크
        if (!deal.getBuyer().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        deal.updateDeliveryAddress(address, addressDetail, postalCode);
        liveDealRepository.save(deal);
    }

    @Transactional
    public void cancelDueToPaymentTimeout(Long dealId) {
        LiveDeal deal = findByIdOrThrow(dealId);

        // 상태 검증 - PENDING 상태만 취소 가능
        if (deal.getStatus() != DealStatus.PENDING) {
            return;
        }

        // Deal 상태 변경
        deal.updateStatus(DealStatus.CANCELLED);

        // LiveItem 상태 변경
        deal.getItem().changeAuctionStatus(AuctionStatus.FAILED);

        // 보증금은 이미 낙찰 시점에 판매자에게 이체되었으므로 추가 처리 불필요

        eventPublisher.publishEvent(
            new PaymentTimeoutEvent(
                deal.getId(),
                deal.getBuyer().getId(),
                deal.getItem().getSellerUserId(),
                deal.getItem().getId(),
                deal.getItem().getName(),
                deal.getWinningPrice()
            )
        );
    }

    // 구매 확정
    @Transactional
    public void confirmPurchase(User currentUser, Long dealId) {
        LiveDeal deal = findByIdOrThrow(dealId);

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

        // LiveItem 상태 업데이트
        deal.getItem().changeAuctionStatus(AuctionStatus.PURCHASE_CONFIRMED);

        eventPublisher.publishEvent(
            new TransactionCompleteEvent(
                deal.getId(),
                deal.getBuyer().getId(),
                deal.getItem().getSellerUserId(),
                deal.getItem().getId(),
                AuctionType.LIVE,
                deal.getItem().getName(),
                deal.getWinningPrice()
            )
        );
    }

}
