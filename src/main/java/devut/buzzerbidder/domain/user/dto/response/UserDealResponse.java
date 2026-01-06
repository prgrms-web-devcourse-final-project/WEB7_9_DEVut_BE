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

    @Schema(description = "구매자인지 판매자인지", example = "SELLER")
    Role role,

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
    String deliveryPostalCode,

    @Schema(description = "송장번호", example = "1234567890")
    String trackingNumber,

    @Schema(description = "택배사 코드", example = "kr.cjlogistics")
    String carrierCode
) {
    public static UserDealResponse fromLiveDeal(LiveDeal liveDeal, boolean isBuyer) {
        String imageUrl = liveDeal.getItem().getImages().isEmpty()
            ? null
            : liveDeal.getItem().getImages().get(0).getImageUrl();

        Role role = isBuyer ? Role.BUYER : Role.SELLER;

        return new UserDealResponse(
                liveDeal.getId(),
                liveDeal.getItem().getId(),
                liveDeal.getItem().getName(),
                role,
                liveDeal.getWinningPrice(),
                liveDeal.getStatus(),
                imageUrl,
                liveDeal.getDeliveryAddress(),
                liveDeal.getDeliveryAddressDetail(),
                liveDeal.getDeliveryPostalCode(),
                liveDeal.getTrackingNumber(),
                liveDeal.getCarrier() != null ? liveDeal.getCarrier().getCode() : null
        );
    }

    public static UserDealResponse fromDelayedDeal(DelayedDeal delayedDeal, boolean isBuyer) {
        String imageUrl = delayedDeal.getItem().getImages().isEmpty()
            ? null
            : delayedDeal.getItem().getImages().get(0).getImageUrl();

        Role role = isBuyer ? Role.BUYER : Role.SELLER;

        return new UserDealResponse(
            delayedDeal.getId(),
            delayedDeal.getItem().getId(),
            delayedDeal.getItem().getName(),
            role,
            delayedDeal.getWinningPrice(),
            delayedDeal.getStatus(),
            imageUrl,
            delayedDeal.getDeliveryAddress(),
            delayedDeal.getDeliveryAddressDetail(),
            delayedDeal.getDeliveryPostalCode(),
            delayedDeal.getTrackingNumber(),
            delayedDeal.getCarrier() != null ? delayedDeal.getCarrier().getCode() : null
        );
    }

    private enum Role {
        SELLER, BUYER
    }
}

