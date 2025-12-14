package devut.buzzerbidder.global.notification.enums;

/**
 * 경매 실시간 알림 타입
 */
public enum AuctionNotificationType {
    NEW_BID,           // 새로운 입찰
    BID_UPDATED,       // 입찰가 갱신
    AUCTION_CLOSING,   // 경매 마감 임박
    AUCTION_ENDED,     // 경매 종료
    ITEM_SOLD,         // 낙찰
    USER_JOINED,       // 사용자 입장
    USER_LEFT          // 사용자 퇴장
}
