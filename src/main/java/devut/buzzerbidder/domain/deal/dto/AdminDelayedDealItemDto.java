package devut.buzzerbidder.domain.deal.dto;

import devut.buzzerbidder.domain.deal.entity.DelayedDeal;

import java.time.LocalDateTime;

public record AdminDelayedDealItemDto(
        Long dealId,
        Long itemId,
        String itemName,
        Long sellerUserId,
        Long buyerId,
        Long winningPrice,
        String status,
        String trackingNumber,
        String carrier,
        LocalDateTime createdDate
) {
    public static AdminDelayedDealItemDto from(DelayedDeal deal) {
        return new AdminDelayedDealItemDto(
                deal.getId(),
                deal.getItem().getId(),
                deal.getItem().getName(),
                deal.getItem().getSellerUserId(),
                deal.getBuyer().getId(),
                deal.getWinningPrice(),
                deal.getStatus().name(),
                deal.getTrackingNumber(),
                deal.getCarrier() != null ? deal.getCarrier().name() : null,
                deal.getCreateDate()
        );
    }
}



