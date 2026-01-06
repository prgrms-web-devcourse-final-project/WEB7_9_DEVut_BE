package devut.buzzerbidder.domain.liveitem.dto.response;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import java.time.LocalDateTime;

public record LiveItemResponse(
    Long id,
    String name,
    String image,
    LocalDateTime startAt,
    AuctionStatus auctionStatus,
    Long initPrice,
    Long currentPrice,
    Boolean isLiked
) {
    public LiveItemResponse(LiveItem liveItem,Boolean isLiked ) {
        this(
            liveItem.getId(),
            liveItem.getName(),
            liveItem.getThumbnail(),
            liveItem.getLiveTime(),
            liveItem.getAuctionStatus(),
            liveItem.getInitPrice(),
            liveItem.getCurrentPrice(),
            isLiked
        );
    }
}
