package devut.buzzerbidder.domain.liveitem.dto.request;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.Category;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.ItemStatus;
import java.time.LocalDateTime;
import java.util.List;

public record LiveItemCreateRequest(

    String name,
    Category category,
    ItemStatus itemStatus,
    String description,
    Long initPrice,
    Boolean deliveryInclude,
    LocalDateTime liveTime,
    Boolean directDealAvailable,
    String region,
    String preferredPlace,
    AuctionStatus auctionStatus,
    List<String> images
    ) {

}
