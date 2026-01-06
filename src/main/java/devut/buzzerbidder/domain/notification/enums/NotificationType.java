package devut.buzzerbidder.domain.notification.enums;

public enum NotificationType {
    DELAYED_FIRST_BID,              // 지연 경매 첫 입찰 알림
    DELAYED_BID_OUTBID,             // 지연 경매 내 입찰가가 밀림
    DELAYED_SUCCESS_BIDDER,         // 지연 경매 입찰자에게 낙찰 알림
    DELAYED_SUCCESS_SELLER,         // 지연 경매 판매자에게 낙찰 알림
    DELAYED_FAILED_SELLER,          // 지연 경매 판매자에게 유찰 알림
    DELAYED_BUY_NOW_SOLD,           // 지연 경매 판매자에게 즉시 구매 알림
    DELAYED_CANCELLED_BY_BUY_NOW,   // 지연 경매 이전 최고 입찰자에게 즉시 구매로 인한 취소 알림
    DM_FIRST_MESSAGE,               // 지연 상품 첫 DM 알림
    LIVE_AUCTION_START,             // 찜한 상품 라이브 경매 시작
    LIVE_SUCCESS_BIDDER,            // 라이브 경매 낙찰자에게 낙찰 알림
    LIVE_SUCCESS_SELLER,            // 라이브 경매 판매자에게 낙찰 알림
    LIVE_FAILED_SELLER,             // 라이브 경매 판매자에게 유찰 알림
    PAYMENT_REMINDER,               // 잔금 리마인더 알림
    PAYMENT_COMPLETE,               // 결제 완료
    ITEM_SHIPPED,                   // 상품 배송 시작
    TRANSACTION_COMPLETE,           // 거래 완료
}
