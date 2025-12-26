package devut.buzzerbidder.domain.auctionroom.event;

import java.time.LocalDateTime;
import java.util.List;

public record AuctionRoomStartedEvent(
    Long roomId,
    LocalDateTime liveTime,
    List<Long> liveItemIds
) {

}
