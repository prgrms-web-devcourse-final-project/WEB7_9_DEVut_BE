package devut.buzzerbidder.domain.liveBid.service;

import devut.buzzerbidder.domain.auctionroom.repository.AuctionRoomRepository;
import devut.buzzerbidder.domain.liveBid.dto.LiveBidEvent;
import devut.buzzerbidder.domain.liveBid.entity.LiveBidLog;
import devut.buzzerbidder.domain.liveBid.repository.LiveBidRepository;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveBidKafkaConsumer {

    private final LiveBidRepository liveBidRepository;
    private final LiveItemRepository liveItemRepository;
    private final UserRepository userRepository;
    private final AuctionRoomRepository auctionRoomRepository;

    @KafkaListener(topics = "live-bid-events", groupId = "bid-storage-group")
    public void consume(LiveBidEvent event) {
        log.info("입찰 로그 DB 저장 시작 - Item: {}, Price: {}", event.liveItemId(), event.bidPrice());

        LiveBidLog logEntity = LiveBidLog.builder()
                .bidder(userRepository.getReferenceById(event.bidderId()))
                .liveItem(liveItemRepository.getReferenceById(event.liveItemId()))
                .seller(userRepository.getReferenceById(event.sellerId()))
                .auctionRoom(auctionRoomRepository.getReferenceById(event.auctionRoomId()))
                .bidPrice(event.bidPrice())
                .build();

        liveBidRepository.save(logEntity);

        //TODO: DB 저장 실패 시 재시도 로직 구현
    }
}
