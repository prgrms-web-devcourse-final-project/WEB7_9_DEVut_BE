package devut.buzzerbidder.domain.liveitem.dto.request;


import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.Category;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.ItemStatus;
import java.time.LocalDateTime;
import java.util.List;

public record LiveItemModifyRequest(

    Long auctionId,
    String name,
    Category category,
    String description,
    Integer initPrice,
    ItemStatus Itemstatus,
    AuctionStatus auctionStatus,
    LocalDateTime liveDate,
    Boolean directDealAvailable,
    String region,
    String preferredPlace,
    List<String> images
) {

}