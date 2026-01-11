package devut.buzzerbidder.domain.deal.service;

import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.AuctionType;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.event.ItemShippedEvent;
import devut.buzzerbidder.domain.deal.event.PaymentCompleteEvent;
import devut.buzzerbidder.domain.deal.event.PaymentTimeoutEvent;
import devut.buzzerbidder.domain.deal.event.TransactionCompleteEvent;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.deliveryTracking.dto.response.DeliveryTrackingResponse;
import devut.buzzerbidder.domain.deliveryTracking.service.DeliveryTrackingService;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.entity.DeliveryAddress;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.DeliveryAddressRepository;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveDealService {

    private final LiveDealRepository liveDealRepository;
    private final LiveItemRepository liveItemRepository;
    private final DeliveryTrackingService deliveryTrackingService;
    private final UserService userService;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;
    private final DeliveryAddressRepository deliveryAddressRepository;

    public LiveDeal findByIdOrThrow(Long dealId) {
        return liveDealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));
    }

    @Transactional
    public void createDeal(Long liveItemId, Long buyerId, Long winningPrice, Long depositAmount) {
        LiveItem item = liveItemRepository.findByIdWithLock(liveItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));
        User buyer = userService.findById(buyerId);

        // 구매자의 기본 배송지 조회
        DeliveryAddress defaultAddress = null;
        if (buyer.getDefaultDeliveryAddressId() != null) {
            defaultAddress = deliveryAddressRepository.findByUserAndId(buyer, buyer.getDefaultDeliveryAddressId())
                    .orElse(null);
        }
        if (defaultAddress == null) {
            defaultAddress = deliveryAddressRepository.findByUserAndIsDefaultTrue(buyer)
                    .orElse(null);
        }

        // LiveDeal 생성
        LiveDeal deal = LiveDeal.builder()
                .item(item)
                .buyer(buyer)
                .winningPrice(winningPrice)
                .depositAmount(depositAmount)
                .status(DealStatus.PENDING)
                .deliveryAddress(defaultAddress != null ? defaultAddress.getAddress() : null)
                .deliveryAddressDetail(defaultAddress != null ? defaultAddress.getAddressDetail() : null)
                .deliveryPostalCode(defaultAddress != null ? defaultAddress.getPostalCode() : null)
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

        LiveDeal liveDeal = findByIdOrThrow(dealId);

        boolean isSeller = liveDeal.getItem().getSellerUserId().equals(currentUser.getId());
        boolean isBuyer = liveDeal.getBuyer().getId().equals(currentUser.getId());
        if (!isSeller && !isBuyer) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

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
    public void completePayment(User currentUser, Long dealId) {
        LiveDeal deal = findByIdOrThrow(dealId);

        // 구매자 권한 체크
        if (!deal.getBuyer().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 상태 체크 - PENDING 상태여야 결제 가능
        if (deal.getStatus() != DealStatus.PENDING) {
            throw new BusinessException(ErrorCode.DEAL_INVALID_STATUS);
        }

        // 잔금 계산
        Long remainingAmount = deal.getWinningPrice() - deal.getDepositAmount();

        // 판매자 조회
        User seller = userService.findById(deal.getItem().getSellerUserId());

        // Wallet에서 잔금 이체 (구매자 -> 판매자)
        walletService.transferBizz(currentUser, seller, remainingAmount);

        // Deal 상태 변경
        deal.updateStatus(DealStatus.PAID);

        // LiveItem 상태 변경
        deal.getItem().changeAuctionStatus(AuctionStatus.IN_DEAL);

        eventPublisher.publishEvent(
            new PaymentCompleteEvent(
                deal.getId(),
                deal.getBuyer().getId(),
                deal.getItem().getSellerUserId(),
                deal.getItem().getId(),
                deal.getItem().getName(),
                deal.getWinningPrice(),
                deal.getDepositAmount(),
                remainingAmount
            )
        );
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
