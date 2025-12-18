package devut.buzzerbidder.domain.liveitem.dto.response;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.Category;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.ItemStatus;
import java.time.LocalDateTime;
import java.util.List;

public record LiveItemDetailResponse(
    Long id,
    Long sellerId,
    String name,
    Category category,
    String description,
    Boolean deliveryInclude,
    ItemStatus itemStatus,
    AuctionStatus auctionStatus,
    LocalDateTime liveTIem,
    Boolean DirectDealAvailable,
    String region,
    String preferredPlace,
    List<String> images,
    Long likeCount
) {

}