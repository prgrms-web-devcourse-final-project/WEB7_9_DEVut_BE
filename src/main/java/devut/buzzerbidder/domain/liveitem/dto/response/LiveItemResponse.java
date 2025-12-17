package devut.buzzerbidder.domain.liveitem.dto.response;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import java.time.LocalDateTime;

public record LiveItemResponse(
    Long id,
    String name,
    String image,
    LocalDateTime liveTime
) {
    public LiveItemResponse(LiveItem liveItem) {
        this(
            liveItem.getId(),
            liveItem.getName(),
            liveItem.getImages().get(0).getImageUrl(),
            liveItem.getLiveTime()
        );
    }
}