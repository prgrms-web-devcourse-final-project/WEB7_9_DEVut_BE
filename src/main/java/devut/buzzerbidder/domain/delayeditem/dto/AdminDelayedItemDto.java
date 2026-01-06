package devut.buzzerbidder.domain.delayeditem.dto;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;

import java.time.LocalDateTime;

public record AdminDelayedItemDto(
        Long id,
        Long sellerUserId,
        String name,
        DelayedItem.Category category,
        Long startPrice,
        Long currentPrice,
        Long buyNowPrice,
        DelayedItem.AuctionStatus auctionStatus,
        LocalDateTime createDate
) {
    public static AdminDelayedItemDto from(DelayedItem item) {
        return new AdminDelayedItemDto(
                item.getId(),
                item.getSellerUserId(),
                item.getName(),
                item.getCategory(),
                item.getStartPrice(),
                item.getCurrentPrice(),
                item.getBuyNowPrice(),
                item.getAuctionStatus(),
                item.getCreateDate()
        );
    }
}

