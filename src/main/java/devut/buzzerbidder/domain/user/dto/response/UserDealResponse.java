package devut.buzzerbidder.domain.user.dto.response;

import devut.buzzerbidder.domain.deal.enums.DealStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래 내역 상세 응답")
public record UserDealResponse(
    @Schema(description = "거래 ID", example = "1")
    Long id,

    @Schema(description = "물품 ID", example = "1")
    Long itemId,

    @Schema(description = "물품 이름", example = "물품 이름")
    String itemName,

    @Schema(description = "판매자 이름", example = "판매자 이름")
    String sellerName,

    @Schema(description = "구매자 이름", example = "구매자 이름")
    String buyerName,

    @Schema(description = "낙찰가", example = "100000")
    Long winningPrice,

    @Schema(description = "거래 상태", example = "COMPLETED")
    DealStatus status,

    @Schema(description = "이미지 URL", example = "https://cdn.example.com/items/delayed/1.jpg")
    String image
) {
}

