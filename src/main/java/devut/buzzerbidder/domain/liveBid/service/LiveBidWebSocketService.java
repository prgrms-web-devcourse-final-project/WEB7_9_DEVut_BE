package devut.buzzerbidder.domain.liveBid.service; // 위치 조정 필요

import devut.buzzerbidder.domain.liveBid.dto.response.AuctionEndMessage;
import devut.buzzerbidder.domain.liveBid.dto.response.BidUpdateMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LiveBidWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String AUCTION_PREFIX = "/receive/auction/";

    /**
     * 입찰 성공 시, 연결된 모든 클라이언트에게 새로운 최고가 정보를 브로드캐스트.
     *
     * @param auctionRoomId 메시지 토픽을 구분하는 경매방 ID
     * @param liveItemId    실제 경매 상품 ID
     * @param newPrice      새로운 최고가
     * @param bidderId      새로운 최고 입찰자 ID
     */
    public void broadcastNewBid(Long auctionRoomId, Long liveItemId, int newPrice, Long bidderId) {
        // 전송할 메시지 구조
        BidUpdateMessage update = new BidUpdateMessage(liveItemId, newPrice, bidderId);

        String destination = AUCTION_PREFIX + auctionRoomId;

        // 구독자들에게 메시지 전송
        messagingTemplate.convertAndSend(destination, update);
    }

    /**
     * 경매 종료시, 연결된 모든 클라이언트에게 경매 종료 결과를 브로드캐스트.
     *
     * @param auctionRoomId 메시지 토픽을 구분하는 경매방 ID
     * @param liveItemId    실제 경매 상품 ID
     * @param success       낙찰 여부 (true: 낙찰, false: 유찰)
     * @param winnerId      낙찰자 ID (유찰시 null)
     * @param finalPrice    낙찰가 (유찰시 null)
     */
    public void broadcastAuctionEnd(Long auctionRoomId, Long liveItemId,
        boolean success, Long winnerId, Integer finalPrice) {
        // 전송할 메시지 구조
        String result = success ? "SUCCESS" : "FAILED";
        AuctionEndMessage message = new AuctionEndMessage(liveItemId, result, winnerId, finalPrice);

        String destination = AUCTION_PREFIX + auctionRoomId;

        // 구독자들에게 메시지 전송
        messagingTemplate.convertAndSend(destination, message);
    }

}


