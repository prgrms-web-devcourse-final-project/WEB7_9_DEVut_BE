package devut.buzzerbidder.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "거래 내역 목록 응답")
public record UserDealListResponse(
    @Schema(description = "거래 내역 목록")
    List<UserDealItemResponse> items,
    @Schema(description = "전체 개수")
    long totalCount
) {
}

