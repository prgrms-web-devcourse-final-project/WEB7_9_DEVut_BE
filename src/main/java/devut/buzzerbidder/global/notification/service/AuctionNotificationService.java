package devut.buzzerbidder.global.notification.service;

import devut.buzzerbidder.global.notification.dto.AuctionNotificationResponse;
import devut.buzzerbidder.global.notification.enums.AuctionNotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 경매 실시간 알림 서비스 (WebSocket STOMP)
 */
@Service
@RequiredArgsConstructor
public class AuctionNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    // TODO: 나중에 DB 저장이 필요하면 NotificationService 의존성 추가

    // 경매 알림 브로드캐스트 경로
    private static final String AUCTION_NOTIFICATION_PREFIX = "/receive/auction/";

    /**
     * 특정 경매방의 모든 참가자에게 알림 전송
     *
     * @param auctionId 경매 ID
     * @param response 알림 내용
     */
    public void sendToAuction(Long auctionId, AuctionNotificationResponse response) {
        String destination = AUCTION_NOTIFICATION_PREFIX + auctionId;
        messagingTemplate.convertAndSend(destination, response);
    }

    /**
     * 특정 사용자에게만 알림 전송
     *
     * @param userId 사용자 ID
     * @param response 알림 내용
     */
    private void sendToUser(Long userId, AuctionNotificationResponse response) {
        String destination = "/receive/user/" + userId + "/auction";
        messagingTemplate.convertAndSend(destination, response);
    }

    /**
     * 새로운 입찰 알림
     */
    public void notifyNewBid(Long auctionId, Integer bidAmount, String bidderNickname) {
        AuctionNotificationResponse notification = AuctionNotificationResponse.of(
                AuctionNotificationType.NEW_BID,
                bidderNickname + "님이 " + bidAmount + "원에 입찰했습니다",
                bidAmount,
                bidderNickname
        );
        sendToAuction(auctionId, notification);
    }

    /**
     * 입찰가 갱신 알림 (직전 최고 입찰자에게만 전송)
     *
     * @param previousBidderId 직전 최고 입찰자 ID
     * @param newBidAmount 새로운 입찰가
     * @param newBidderNickname 새로운 입찰자 닉네임
     */
    public void notifyBidUpdated(Long previousBidderId, Integer newBidAmount, String newBidderNickname) {
        AuctionNotificationResponse notification = AuctionNotificationResponse.of(
                AuctionNotificationType.BID_UPDATED,
                newBidderNickname + "님이 " + newBidAmount + "원에 입찰하여 최고가가 갱신되었습니다",
                newBidAmount,
                newBidderNickname
        );
        sendToUser(previousBidderId, notification);
    }

    /**
     * 경매 마감 임박 알림
     */
    public void notifyAuctionClosing(Long auctionId, Integer currentBid, int remainingMinutes) {
        AuctionNotificationResponse notification = AuctionNotificationResponse.of(
                AuctionNotificationType.AUCTION_CLOSING,
                "경매가 " + remainingMinutes + "분 후 마감됩니다",
                currentBid,
                null
        );
        sendToAuction(auctionId, notification);
    }

    /**
     * 경매 종료 알림
     */
    public void notifyAuctionEnded(Long auctionId, Integer finalBid, String winnerNickname) {
        AuctionNotificationResponse notification = AuctionNotificationResponse.of(
                AuctionNotificationType.AUCTION_ENDED,
                "경매가 종료되었습니다",
                finalBid,
                winnerNickname
        );
        sendToAuction(auctionId, notification);
    }

    /**
     * 낙찰 알림
     */
    public void notifyItemSold(Long auctionId, Integer soldPrice, String winnerNickname) {
        AuctionNotificationResponse notification = AuctionNotificationResponse.of(
                AuctionNotificationType.ITEM_SOLD,
                winnerNickname + "님이 " + soldPrice + "원에 낙찰받았습니다",
                soldPrice,
                winnerNickname
        );
        sendToAuction(auctionId, notification);
    }

    /**
     * 사용자 입장 알림
     */
    public void notifyUserJoined(Long auctionId, String nickname) {
        AuctionNotificationResponse notification = AuctionNotificationResponse.of(
                AuctionNotificationType.USER_JOINED,
                nickname + "님이 경매에 참여했습니다",
                null,
                nickname
        );
        sendToAuction(auctionId, notification);
    }

    /**
     * 사용자 퇴장 알림
     */
    public void notifyUserLeft(Long auctionId, String nickname) {
        AuctionNotificationResponse notification = AuctionNotificationResponse.of(
                AuctionNotificationType.USER_LEFT,
                nickname + "님이 경매를 떠났습니다",
                null,
                nickname
        );
        sendToAuction(auctionId, notification);
    }
}
