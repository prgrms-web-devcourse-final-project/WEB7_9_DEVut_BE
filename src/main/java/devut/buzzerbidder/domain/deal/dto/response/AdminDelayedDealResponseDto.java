package devut.buzzerbidder.domain.deal.dto.response;

import devut.buzzerbidder.domain.deal.dto.AdminDelayedDealItemDto;
import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminDelayedDealResponseDto(
        List<AdminDelayedDealItemDto> deals,
        Integer totalPage,
        Long totalElements,
        Integer currentPage
) {
    public static AdminDelayedDealResponseDto from(List<AdminDelayedDealItemDto> deals, Page<DelayedDeal> page) {
        return new AdminDelayedDealResponseDto(
                deals,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber()
        );
    }
}



