package devut.buzzerbidder.domain.user.dto.response;

import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import devut.buzzerbidder.domain.deal.entity.LiveDeal;
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

    @Schema(description = "구매자 이름", example = "구매자 이름")
    String buyerName,

    @Schema(description = "낙찰가", example = "100000")
    Long winningPrice,

    @Schema(description = "이미지 URL", example = "https://cdn.example.com/items/live/1.jpg")
    String image,

    @Schema(description = "찜 여부", example = "true")
    Boolean wish,

    @Schema(description = "경매 상태", example = "IN_DEAL")
    String auctionStatus,

    @Schema(description = "배송지 주소", example = "서울시 중구 세종대로 135-5")
    String deliveryAddress,

    @Schema(description = "배송지 상세주소", example = "OO아파트 101동 101호")
    String deliveryAddressDetail,

    @Schema(description = "배송지 우편번호", example = "12345")
    String deliveryPostalCode
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
            liveDeal.getBuyer().getNickname(),
            liveDeal.getWinningPrice(),
            imageUrl,
            wish,
            liveDeal.getItem().getAuctionStatus() != null ? liveDeal.getItem().getAuctionStatus().name() : null,
            liveDeal.getDeliveryAddress(),
            liveDeal.getDeliveryAddressDetail(),
            liveDeal.getDeliveryPostalCode()
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
            delayedDeal.getBuyer().getNickname(),
            delayedDeal.getWinningPrice(),
            imageUrl,
            wish,
            delayedDeal.getItem().getAuctionStatus() != null ? delayedDeal.getItem().getAuctionStatus().name() : null,
            delayedDeal.getDeliveryAddress(),
            delayedDeal.getDeliveryAddressDetail(),
            delayedDeal.getDeliveryPostalCode()
        );
    }

}

