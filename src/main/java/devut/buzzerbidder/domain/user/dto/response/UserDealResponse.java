package devut.buzzerbidder.domain.user.dto.response;

import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import devut.buzzerbidder.domain.deal.entity.LiveDeal;
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

    @Schema(description = "구매자 이름", example = "구매자 이름")
    String buyerName,

    @Schema(description = "낙찰가", example = "100000")
    Long winningPrice,

    @Schema(description = "거래 상태", example = "COMPLETED")
    DealStatus status,

    @Schema(description = "이미지 URL", example = "https://cdn.example.com/items/delayed/1.jpg")
    String image,

    @Schema(description = "배송지 주소", example = "서울시 중구 세종대로 135-5")
    String deliveryAddress,

    @Schema(description = "배송지 상세주소", example = "OO아파트 101동 101호")
    String deliveryAddressDetail,

    @Schema(description = "배송지 우편번호", example = "12345")
    String deliveryPostalCode
) {
    public static UserDealResponse fromLiveDeal(LiveDeal liveDeal) {
        String imageUrl = liveDeal.getItem().getImages().isEmpty()
            ? null
            : liveDeal.getItem().getImages().get(0).getImageUrl();

        return new UserDealResponse(
            liveDeal.getId(),
            liveDeal.getItem().getId(),
            liveDeal.getItem().getName(),
            liveDeal.getBuyer().getNickname(),
            liveDeal.getWinningPrice(),
            liveDeal.getStatus(),
            imageUrl,
            liveDeal.getDeliveryAddress(),
            liveDeal.getDeliveryAddressDetail(),
            liveDeal.getDeliveryPostalCode()
        );
    }

    public static UserDealResponse fromDelayedDeal(DelayedDeal delayedDeal) {
        String imageUrl = delayedDeal.getItem().getImages().isEmpty()
            ? null
            : delayedDeal.getItem().getImages().get(0).getImageUrl();

        return new UserDealResponse(
            delayedDeal.getId(),
            delayedDeal.getItem().getId(),
            delayedDeal.getItem().getName(),
            delayedDeal.getBuyer().getNickname(),
            delayedDeal.getWinningPrice(),
            delayedDeal.getStatus(),
            imageUrl,
            delayedDeal.getDeliveryAddress(),
            delayedDeal.getDeliveryAddressDetail(),
            delayedDeal.getDeliveryPostalCode()
        );
    }
}

