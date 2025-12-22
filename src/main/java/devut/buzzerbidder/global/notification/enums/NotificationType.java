package devut.buzzerbidder.global.notification.enums;

public enum NotificationType {
    AUCTION_START,              // 관심 경매 시작
    DELAYED_BID_OUTBID,         // 지연 경매 내 입찰가가 밀림
    AUCTION_SUCCESS_BIDDER,     // 입찰자에게 낙찰 알림
    AUCTION_SUCCESS_SELLER,     // 판매자에게 낙찰 알림
    AUCTION_FAILED_SELLER,      // 판매자에게 유찰 알림
    AUCTION_END,                // 경매 종료
    PAYMENT_COMPLETE,           // 결제 완료
    TRANSACTION_COMPLETE,       // 거래 완료
    MESSAGE_RECEIVED,           // 채팅 메시지
    PAYMENT_REMINDER,           // 결제 알림
    ITEM_SHIPPED                // 배송 시작
}
