package devut.buzzerbidder.domain.deal.dto.response;

import devut.buzzerbidder.domain.deal.dto.AdminDelayedDealItemDto;
import devut.buzzerbidder.domain.deal.dto.AdminLiveDealItemDto;
import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public record AdminLiveDealResponseDto(
        List<AdminLiveDealItemDto> deals,
        Integer totalPage,
        Long totalElements,
        Integer currentPage
) {
    public static AdminLiveDealResponseDto from(List<AdminLiveDealItemDto> deals, Page<LiveDeal> page) {
        return new AdminLiveDealResponseDto(
                deals,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber()
        );
    }
}
