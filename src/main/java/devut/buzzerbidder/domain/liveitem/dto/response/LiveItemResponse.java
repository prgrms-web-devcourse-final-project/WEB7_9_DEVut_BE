package devut.buzzerbidder.domain.liveitem.dto.response;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import java.time.LocalDateTime;

public record LiveItemResponse(
    String name,
    String image,
    LocalDateTime liveDate
) {
    public LiveItemResponse(LiveItem liveItem) {
        this(
            liveItem.getName(),
            liveItem.getImages().get(0).getImageUrl(),
            liveItem.getLiveDate()
        );
    }
}