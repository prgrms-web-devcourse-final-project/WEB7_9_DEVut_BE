package devut.buzzerbidder.domain.liveitem.dto.response;

import java.time.LocalDateTime;

public record RoomCreateResponse(
    Long roomId,
    long roomIndex,
    LocalDateTime startAt
) {

}
