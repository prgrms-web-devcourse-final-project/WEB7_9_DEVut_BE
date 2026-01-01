package devut.buzzerbidder.domain.liveitem.dto.request;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem.Category;

public record LiveItemSearchRequest(
    String name,
    Category category,
    Long minBidPrice,
    Long maxBidPrice,
    Boolean isSaling
){
}
