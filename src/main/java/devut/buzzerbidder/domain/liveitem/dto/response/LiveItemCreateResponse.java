package devut.buzzerbidder.domain.liveitem.dto.response;

public record LiveItemCreateResponse(
    ItemCreateResponse item,
    RoomCreateResponse room
) {

}
