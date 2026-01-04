package devut.buzzerbidder.domain.delayeditem.dto.response;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.AuctionStatus;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.Category;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.ItemStatus;
import java.time.LocalDateTime;
import java.util.List;

public record DelayedItemDetailResponse(
    Long id,
    String name,
    Category category,
    String description,
    ItemStatus itemStatus,
    AuctionStatus auctionStatus,
    Long startPrice,
    Long currentPrice,
    Long buyNowPrice,
    LocalDateTime endTime,
    Boolean deliveryInclude,
    Boolean directDealAvailable,
    String region,
    String preferredPlace,
    List<String> images,
    Long sellerUserId,
    Long likeCount,
    Boolean isLiked
) {

}
