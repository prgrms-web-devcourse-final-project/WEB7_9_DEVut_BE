package devut.buzzerbidder.domain.liveitem.dto.response;

import java.time.LocalDateTime;

public record RoomDto(
    Long roomId,
    long roomIndex,
    LocalDateTime startAt
) {

}
