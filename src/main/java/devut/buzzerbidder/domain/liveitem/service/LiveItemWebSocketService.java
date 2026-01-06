package devut.buzzerbidder.domain.liveitem.service;

import devut.buzzerbidder.domain.liveitem.dto.response.AuctionEndMessage;
import devut.buzzerbidder.domain.liveitem.dto.response.AuctionStartMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LiveItemWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String AUCTION_PREFIX = "/receive/auction/";

    /**
     * 경매 시작시, 연결된 모든 클라이언트에게 경매 시작 정보를 브로드캐스트.
     * @param auctionRoomId 메시지 토픽을 구분하는 경매방 ID
     * @param liveItemId    실제 경매 상품 ID
     * @param itemName      상품명
     * @param initPrice     시작가
     */
    public void broadcastAuctionStart(Long auctionRoomId, Long liveItemId,
        String itemName, Integer initPrice) {

        AuctionStartMessage message = new AuctionStartMessage(
            liveItemId, itemName, initPrice
        );

        String destination = AUCTION_PREFIX + auctionRoomId;

        messagingTemplate.convertAndSend(destination, message);
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
        String liveItemName, boolean success, Long winnerId, Integer finalPrice) {
        // 전송할 메시지 구조
        String result = success ? "SUCCESS" : "FAILED";
        AuctionEndMessage message = new AuctionEndMessage(liveItemId, liveItemName, result, winnerId, finalPrice);

        String destination = AUCTION_PREFIX + auctionRoomId;

        // 구독자들에게 메시지 전송
        messagingTemplate.convertAndSend(destination, message);
    }

}
