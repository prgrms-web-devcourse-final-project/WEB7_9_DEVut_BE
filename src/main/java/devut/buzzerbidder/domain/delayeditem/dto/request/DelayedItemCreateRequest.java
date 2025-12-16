package devut.buzzerbidder.domain.delayeditem.dto.request;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.AuctionStatus;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.Category;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.ItemStatus;
import java.time.LocalDateTime;
import java.util.List;

public record DelayedItemCreateRequest(
    String name,
    Category category,
    String description,
    Long startPrice,
    LocalDateTime endTime,
    ItemStatus itemStatus,
    Boolean deliveryInclude,
    Boolean directDealAvailable,
    String region,
    String preferredPlace,
    List<String> images
) {

}
