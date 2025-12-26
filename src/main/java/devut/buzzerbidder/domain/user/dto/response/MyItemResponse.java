package devut.buzzerbidder.domain.user.dto.response;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import java.time.LocalDateTime;

public record MyItemResponse(
    Long id,
    String type,
    String name,
    String category,
    Long currentPrice,
    Long initPrice,
    Long instantBuyPrice,
    Boolean wish,
    String image,
    String auctionStatus,
    LocalDateTime endTime,
    LocalDateTime createdAt
) {
    public static MyItemResponse fromLiveItem(LiveItem liveItem, Long currentPrice, Boolean wish) {
        String imageUrl = liveItem.getImages().isEmpty() 
            ? null 
            : liveItem.getImages().get(0).getImageUrl();
        
        return new MyItemResponse(
            liveItem.getId(),
            "LIVE",
            liveItem.getName(),
            liveItem.getCategory() != null ? liveItem.getCategory().name() : null,
            currentPrice,
            liveItem.getInitPrice(),
            null, // LiveItem에는 instantBuyPrice가 없음
            wish,
            imageUrl,
            liveItem.getAuctionStatus() != null ? liveItem.getAuctionStatus().name() : null,
            liveItem.getLiveTime(), // 라이브 경매는 liveTime을 endTime으로 사용
            liveItem.getCreateDate()
        );
    }

    public static MyItemResponse fromDelayedItem(DelayedItem delayedItem, Boolean wish) {
        String imageUrl = delayedItem.getImages().isEmpty() 
            ? null 
            : delayedItem.getImages().get(0).getImageUrl();
        
        return new MyItemResponse(
            delayedItem.getId(),
            "DELAYED",
            delayedItem.getName(),
            delayedItem.getCategory() != null ? delayedItem.getCategory().name() : null,
            delayedItem.getCurrentPrice(),
            delayedItem.getStartPrice(),
            delayedItem.getBuyNowPrice(), // DelayedItem의 buyNowPrice를 instantBuyPrice로 사용
            wish,
            imageUrl,
            delayedItem.getAuctionStatus() != null ? delayedItem.getAuctionStatus().name() : null,
            delayedItem.getEndTime(), // 지연 경매는 endTime 사용
            delayedItem.getCreateDate()
        );
    }
}
