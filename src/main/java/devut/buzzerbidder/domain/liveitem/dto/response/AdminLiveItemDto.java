package devut.buzzerbidder.domain.liveitem.dto.response;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem;

import java.time.LocalDateTime;

public record AdminLiveItemDto(
        Long id,
        Long sellerUserId,
        String name,
        LiveItem.Category category,
        Long initPrice,
        LiveItem.AuctionStatus auctionStatus,
        LocalDateTime createDate
) {
    public static AdminLiveItemDto from(LiveItem item) {
        return new AdminLiveItemDto(
                item.getId(),
                item.getSellerUserId(),
                item.getName(),
                item.getCategory(),
                item.getInitPrice(),
                item.getAuctionStatus(),
                item.getCreateDate()
        );
    }
}
