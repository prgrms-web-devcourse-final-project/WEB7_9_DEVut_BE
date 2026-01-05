package devut.buzzerbidder.domain.liveitem.dto.response;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminLiveItemResponseDto(
        List<AdminLiveItemDto> deals,
        Integer totalPage,
        Long totalElements,
        Integer currentPage
) {
    public static AdminLiveItemResponseDto from(List<AdminLiveItemDto> items, Page<LiveItem> page) {
        return new AdminLiveItemResponseDto(
                items,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber()
        );
    }
}
