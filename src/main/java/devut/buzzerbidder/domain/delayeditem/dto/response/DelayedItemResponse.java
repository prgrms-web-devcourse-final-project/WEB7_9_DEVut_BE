package devut.buzzerbidder.domain.delayeditem.dto.response;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.AuctionStatus;
import java.time.LocalDateTime;

public record DelayedItemResponse(
    Long id,
    String name,
    String image,
    Long currentPrice,
    Long buyNowPrice,
    LocalDateTime endTime,
    AuctionStatus auctionStatus,
    Boolean isLiked
) {
    public DelayedItemResponse(DelayedItem delayedItem) {
        this(
            delayedItem.getId(),
            delayedItem.getName(),
            delayedItem.getImages().isEmpty() ? null : delayedItem.getImages().getFirst().getImageUrl(),
            delayedItem.getCurrentPrice(),
            delayedItem.getBuyNowPrice(),
            delayedItem.getEndTime(),
            delayedItem.getAuctionStatus(),
            false
        );
    }

    public DelayedItemResponse(DelayedItem delayedItem, boolean isLiked) {
        this(
            delayedItem.getId(),
            delayedItem.getName(),
            delayedItem.getImages().isEmpty() ? null : delayedItem.getImages().getFirst().getImageUrl(),
            delayedItem.getCurrentPrice(),
            delayedItem.getBuyNowPrice(),
            delayedItem.getEndTime(),
            delayedItem.getAuctionStatus(),
            isLiked
        );
    }
}
