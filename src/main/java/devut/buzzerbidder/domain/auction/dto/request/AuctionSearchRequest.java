package devut.buzzerbidder.domain.auction.dto.request;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem.Category;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuctionSearchRequest(
    @Schema(description = "경매 타입 (LIVE, DELAYED, ALL)")
    String type,

    @Schema(description = "카테고리")
    Category category,

    @Schema(description = "검색 키워드 (상품명)")
    String keyword,

    @Schema(description = "경매 상태 (BEFORE_BIDDING, IN_PROGRESS 등)")
    String status,

    @Schema(description = "최소 가격")
    Long minPrice,

    @Schema(description = "최대 가격")
    Long maxPrice,

    @Schema(description = "참여 가능한 경매 여부(입찰 전, 라이브 대기, 입찰 중")
    Boolean isSelling
) {
    public AuctionSearchRequest {
        if (type == null || type.isBlank()) type = "ALL";
    }
}
