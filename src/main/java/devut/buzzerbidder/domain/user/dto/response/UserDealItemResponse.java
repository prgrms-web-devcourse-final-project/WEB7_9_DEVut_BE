package devut.buzzerbidder.domain.user.dto.response;

import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래 내역 목록 아이템")
public record UserDealItemResponse(
    @Schema(description = "거래 ID", example = "1")
    Long id,

    @Schema(description = "물품 ID", example = "1")
    Long itemId,

    @Schema(description = "경매 타입", example = "LIVE")
    String type,

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

    @Schema(description = "이미지 URL", example = "https://cdn.example.com/items/live/1.jpg")
    String image,

    @Schema(description = "찜 여부", example = "true")
    Boolean wish,

    @Schema(description = "경매 상태", example = "IN_DEAL")
    String auctionStatus
) {
    public static UserDealItemResponse fromLiveDeal(LiveDeal liveDeal, Boolean wish) {
        String imageUrl = liveDeal.getItem().getImages().isEmpty()
            ? null
            : liveDeal.getItem().getImages().get(0).getImageUrl();

        return new UserDealItemResponse(
            liveDeal.getId(),
            liveDeal.getItem().getId(),
            "LIVE",
            liveDeal.getItem().getName(),
            null, // sellerName은 Service에서 조회하여 설정
            liveDeal.getBuyer().getNickname(),
            liveDeal.getWinningPrice(),
            liveDeal.getStatus(),
            imageUrl,
            wish,
            liveDeal.getItem().getAuctionStatus() != null ? liveDeal.getItem().getAuctionStatus().name() : null
        );
    }

    public static UserDealItemResponse fromDelayedDeal(DelayedDeal delayedDeal, Boolean wish) {
        String imageUrl = delayedDeal.getItem().getImages().isEmpty()
            ? null
            : delayedDeal.getItem().getImages().get(0).getImageUrl();

        return new UserDealItemResponse(
            delayedDeal.getId(),
            delayedDeal.getItem().getId(),
            "DELAYED",
            delayedDeal.getItem().getName(),
            null, // sellerName은 Service에서 조회하여 설정
            delayedDeal.getBuyer().getNickname(),
            delayedDeal.getWinningPrice(),
            delayedDeal.getStatus(),
            imageUrl,
            wish,
            delayedDeal.getItem().getAuctionStatus() != null ? delayedDeal.getItem().getAuctionStatus().name() : null
        );
    }

    public UserDealItemResponse withSellerName(String sellerName) {
        return new UserDealItemResponse(
            this.id,
            this.itemId,
            this.type,
            this.itemName,
            sellerName,
            this.buyerName,
            this.winningPrice,
            this.status,
            this.image,
            this.wish,
            this.auctionStatus
        );
    }
}

