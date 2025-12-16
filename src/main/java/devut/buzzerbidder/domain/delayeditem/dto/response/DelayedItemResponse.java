package devut.buzzerbidder.domain.delayeditem.dto.response;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import java.time.LocalDateTime;

public record DelayedItemResponse(
    String name,
    String image,
    Long currentPrice,
    LocalDateTime endTime
) {
    public DelayedItemResponse(DelayedItem delayedItem) {
        this(
            delayedItem.getName(),
            delayedItem.getImages().isEmpty() ? null : delayedItem.getImages().getFirst().getImageUrl(),
            delayedItem.getCurrentPrice(),
            delayedItem.getEndTime()
        );
    }
}
