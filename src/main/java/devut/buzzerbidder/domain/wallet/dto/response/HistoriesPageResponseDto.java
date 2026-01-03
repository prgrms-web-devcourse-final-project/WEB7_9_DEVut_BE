package devut.buzzerbidder.domain.wallet.dto.response;

import devut.buzzerbidder.domain.wallet.entity.WalletHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

public record HistoriesPageResponseDto(
        @Schema(description = "지갑 히스토리 응답DTO 리스트")
        List<HistoryResponseDto> walletHistories,
        @Schema(description = "현재 페이지 번호", example = "3")
        int currentPage,
        @Schema(description = "전체 페이지 수", example = "10")
        int totalPages,
        @Schema(description = "전체 히스토리 개수", example = "95")
        int totalElements,
        @Schema(description = "페이지당 히스토리 개수", example = "10")
        int pageSize
) {
    public static HistoriesPageResponseDto from(Page<WalletHistory> page, List<HistoryResponseDto> walletHistories) {
        return new HistoriesPageResponseDto(
                walletHistories,
                page.getNumber() + 1,
                page.getTotalPages(),
                (int) page.getTotalElements(),
                page.getSize()
        );
    }
}
