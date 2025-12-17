package devut.buzzerbidder.domain.delayeditem.dto.request;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.Category;

public record DelayedItemSearchRequest(
    String name,
    Category category,
    Long minCurrentPrice,
    Long maxCurrentPrice
) {

}
