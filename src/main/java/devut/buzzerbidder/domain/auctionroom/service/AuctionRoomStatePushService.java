package devut.buzzerbidder.domain.auctionroom.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuctionRoomStatePushService {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String AUCTION_STATE_DEST_PREFIX = "/receive/auction/"; // /receive/auction/{auctionRoomId}

    public void pushRefresh(Long auctionRoomId, String reason) {
        messagingTemplate.convertAndSend(
                AUCTION_STATE_DEST_PREFIX + auctionRoomId,
                new AuctionRoomRefreshMessage("AUCTION_REFRESH", reason, System.currentTimeMillis())
        );
    }

    public record AuctionRoomRefreshMessage(
            String type,
            String reason,
            long ts
    ) {}
}
