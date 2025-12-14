package devut.buzzerbidder.global.notification.enums;

public enum NotificationType {
    AUCTION_START,          // 관심 경매 시작
    AUCTION_OUTBID,         // 내 입찰가가 밀림
    AUCTION_WIN,            // 낙찰 성공
    AUCTION_FAILURE,        // 낙찰 실패
    AUCTION_END,            // 경매 종료
    PAYMENT_COMPLETE,       // 결제 완료
    TRANSACTION_COMPLETE,   // 거래 완료
    MESSAGE_RECEIVED,       // 채팅 메시지
    PAYMENT_REMINDER,       // 결제 알림
    ITEM_SHIPPED            // 배송 시작
}
