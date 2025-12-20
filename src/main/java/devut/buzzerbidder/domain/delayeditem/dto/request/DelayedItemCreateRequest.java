package devut.buzzerbidder.domain.delayeditem.dto.request;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.Category;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.ItemStatus;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;

public record DelayedItemCreateRequest(
    String name,
    Category category,
    String description,
    Long startPrice,
    Long buyNowPrice,
    LocalDateTime endTime,
    ItemStatus itemStatus,
    Boolean deliveryInclude,
    Boolean directDealAvailable,
    String region,
    String preferredPlace,
    List<String> images
) {
    public void validate() {
        if (buyNowPrice != null && buyNowPrice <= startPrice) {
            throw new BusinessException(ErrorCode.INVALID_BUY_NOW_PRICE);
        }
    }

}
