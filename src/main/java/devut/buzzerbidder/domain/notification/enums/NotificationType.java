package devut.buzzerbidder.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    // 일반 알림 (읽음: 30일, 안 읽음: 90일)
    DELAYED_FIRST_BID(NotificationTTL.NORMAL),              // 지연 경매 첫 입찰 알림
    DELAYED_BID_OUTBID(NotificationTTL.NORMAL),             // 지연 경매 내 입찰가가 밀림
    DELAYED_FAILED_SELLER(NotificationTTL.NORMAL),          // 지연 경매 판매자에게 유찰 알림
    DELAYED_BUY_NOW_SOLD(NotificationTTL.NORMAL),           // 지연 경매 판매자에게 즉시 구매 알림
    DELAYED_CANCELLED_BY_BUY_NOW(NotificationTTL.NORMAL),   // 지연 경매 이전 최고 입찰자에게 즉시 구매로 인한 취소 알림
    DM_FIRST_MESSAGE(NotificationTTL.NORMAL),               // 지연 상품 첫 DM 알림
    LIVE_AUCTION_START(NotificationTTL.NORMAL),             // 찜한 상품 라이브 경매 시작
    LIVE_FAILED_SELLER(NotificationTTL.NORMAL),             // 라이브 경매 판매자에게 유찰 알림
    PAYMENT_REMINDER(NotificationTTL.NORMAL),               // 잔금 리마인더 알림
    ITEM_SHIPPED(NotificationTTL.NORMAL),                   // 상품 배송 시작

    // 거래/법적 증빙성 알림 (읽음: 365일, 안 읽음: 365일)
    DELAYED_SUCCESS_BIDDER(NotificationTTL.LEGAL),         // 지연 경매 입찰자에게 낙찰 알림
    DELAYED_SUCCESS_SELLER(NotificationTTL.LEGAL),         // 지연 경매 판매자에게 낙찰 알림
    LIVE_SUCCESS_BIDDER(NotificationTTL.LEGAL),            // 라이브 경매 낙찰자에게 낙찰 알림
    LIVE_SUCCESS_SELLER(NotificationTTL.LEGAL),            // 라이브 경매 판매자에게 낙찰 알림
    PAYMENT_TIMEOUT_BUYER(NotificationTTL.LEGAL),          // 잔금 결제 기한 초과로 구매자에게 거래 취소 알림
    PAYMENT_TIMEOUT_SELLER(NotificationTTL.LEGAL),         // 잔금 결제 미완료로 판매자에게 거래 취소 알림
    PAYMENT_COMPLETE(NotificationTTL.LEGAL),               // 잔금 결제 완료
    TRANSACTION_COMPLETE(NotificationTTL.LEGAL);           // 거래 완료

    private final NotificationTTL ttl;

    /**
     * 읽은 알림 보관 기간 (일)
     */
    public int getReadRetentionDays() {
        return ttl.getReadRetentionDays();
    }

    /**
     * 읽지 않은 알림 보관 기간 (일)
     */
    public int getUnreadRetentionDays() {
        return ttl.getUnreadRetentionDays();
    }
}
