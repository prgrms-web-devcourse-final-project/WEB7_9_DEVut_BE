package devut.buzzerbidder.domain.deal.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DealStatus {

    PENDING("거래 생성됨 / 결제 대기"),
    PAID("결제 완료"),
    SHIPPING("판매자 발송 완료"),
    COMPLETED("거래 최종 완료"),

    CANCELLED("결제 전 거래 취소"),
    REFUND_REQUESTED("환불 요청됨"),
    REFUNDED("환불 완료");

    private final String description;

}
