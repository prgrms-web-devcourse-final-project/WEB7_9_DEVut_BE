package devut.buzzerbidder.domain.delayeditem.dto.response;

import java.util.List;

public record DelayedItemListResponse(
    List<DelayedItemResponse> delayedItems,
    long totalCount
) {

}
