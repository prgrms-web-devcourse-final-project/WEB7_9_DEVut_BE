package devut.buzzerbidder.domain.user.dto.response;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import java.time.LocalDateTime;

public record MyItemResponse(
    Long id,
    String type,
    String name,
    String category,
    Long initPrice,
    Long instantBuyPrice,
    Long likes,
    String image,
    LocalDateTime createdAt
) {
    public static MyItemResponse fromLiveItem(LiveItem liveItem, Long likes) {
        String imageUrl = liveItem.getImages().isEmpty() 
            ? null 
            : liveItem.getImages().get(0).getImageUrl();
        
        return new MyItemResponse(
            liveItem.getId(),
            "LIVE",
            liveItem.getName(),
            liveItem.getCategory() != null ? liveItem.getCategory().name() : null,
            liveItem.getInitPrice(),
            null, // LiveItem에는 instantBuyPrice가 없음
            likes,
            imageUrl,
            liveItem.getCreateDate()
        );
    }

    public static MyItemResponse fromDelayedItem(DelayedItem delayedItem, Long likes) {
        String imageUrl = delayedItem.getImages().isEmpty() 
            ? null 
            : delayedItem.getImages().get(0).getImageUrl();
        
        return new MyItemResponse(
            delayedItem.getId(),
            "DELAYED",
            delayedItem.getName(),
            delayedItem.getCategory() != null ? delayedItem.getCategory().name() : null,
            delayedItem.getStartPrice(),
            null, // DelayedItem에는 instantBuyPrice가 없음 (필요시 추가 가능)
            likes,
            imageUrl,
            delayedItem.getCreateDate()
        );
    }
}
