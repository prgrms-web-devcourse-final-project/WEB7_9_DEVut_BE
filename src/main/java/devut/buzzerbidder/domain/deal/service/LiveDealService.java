package devut.buzzerbidder.domain.deal.service;

import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.deliveryTracking.dto.response.DeliveryTrackingResponse;
import devut.buzzerbidder.domain.deliveryTracking.service.DeliveryTrackingService;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
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

    public LiveDeal findByIdOrThrow(Long dealId) {
        return liveDealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));
    }

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
        // TODO: 권한 체크 로직 추가 (currentUser가 해당 deal에 접근할 수 있는지 확인)
        LiveDeal liveDeal = findByIdOrThrow(dealId);
        liveDeal.updateDeliveryInfo(carrierCode, trackingNumber);
        liveDeal.updateStatus(DealStatus.SHIPPING);
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

}
