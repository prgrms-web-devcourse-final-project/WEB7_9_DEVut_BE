package devut.buzzerbidder.domain.delayeditem.dto.response;

import devut.buzzerbidder.domain.delayeditem.dto.AdminDelayedItemDto;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminDelayedItemResponseDto(
        List<AdminDelayedItemDto> deals,
        Integer totalPage,
        Long totalElements,
        Integer currentPage
) {
    public static AdminDelayedItemResponseDto from(List<AdminDelayedItemDto> items, Page<DelayedItem> page) {
        return new AdminDelayedItemResponseDto(
                items,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber()
        );
    }
}
