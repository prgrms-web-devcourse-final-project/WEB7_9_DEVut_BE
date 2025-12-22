package devut.buzzerbidder.domain.liveitem.dto.response;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import java.time.LocalDateTime;

public record LiveItemResponse(
    Long id,
    String name,
    String image,
    LocalDateTime liveTime,
    Long currentPrice,
    AuctionStatus auctionStatus
) {
    public LiveItemResponse(LiveItem liveItem) {
        this(
            liveItem.getId(),
            liveItem.getName(),
            liveItem.getImages().get(0).getImageUrl(),
            liveItem.getLiveTime(),
            liveItem.getInitPrice(),
            liveItem.getAuctionStatus()
        );
    }
}