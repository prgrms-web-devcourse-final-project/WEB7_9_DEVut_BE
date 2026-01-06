package devut.buzzerbidder.domain.deal.dto;

import devut.buzzerbidder.domain.deal.entity.LiveDeal;

import java.time.LocalDateTime;

public record AdminLiveDealItemDto(
        Long dealId,
        Long itemId,
        String itemName,
        Long sellerUserId,
        Long buyerId,
        Long winningPrice,
        String status,
        String trackingNumber,
        String carrier,
        LocalDateTime createdAt
) {
    public static AdminLiveDealItemDto from(LiveDeal deal) {
        return new AdminLiveDealItemDto(
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
