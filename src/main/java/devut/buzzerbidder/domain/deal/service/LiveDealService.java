package devut.buzzerbidder.domain.deal.service;

import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.AuctionType;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LiveDealService {

    private final LiveDealRepository liveDealRepository;

    public LiveDeal findByIdOrThrow(Long dealId) {
        return liveDealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("LiveDeal not found with id: " + dealId));
    }

    public void patchDeliveryInfo(Long dealId, String carrierCode, String trackingNumber) {
        LiveDeal liveDeal = findByIdOrThrow(dealId);
        liveDeal.updateDeliveryInfo(carrierCode, trackingNumber);
    }
}
