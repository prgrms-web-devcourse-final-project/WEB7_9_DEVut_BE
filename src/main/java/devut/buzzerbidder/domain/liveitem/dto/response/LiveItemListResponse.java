package devut.buzzerbidder.domain.liveitem.dto.response;

import java.util.List;

public record LiveItemListResponse(
    List<LiveItemResponse> liveItems,
    long totalCount ) {

    }