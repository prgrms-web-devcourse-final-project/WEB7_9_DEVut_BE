package devut.buzzerbidder.domain.deal.service;

import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.deliveryTracking.dto.response.DeliveryTrackingResponse;
import devut.buzzerbidder.domain.deliveryTracking.service.DeliveryTrackingService;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveDealService {

    private final LiveDealRepository liveDealRepository;
    private final DeliveryTrackingService deliveryTrackingService;

    public LiveDeal findByIdOrThrow(Long dealId) {
        return liveDealRepository.findById(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));
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
