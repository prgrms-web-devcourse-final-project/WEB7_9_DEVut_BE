package devut.buzzerbidder.domain.liveBid.service;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class liveBidService {

    private final LiveItemRepository liveItemRepository;
    private final BidRedisService bidRedisService;

    // 경매 시작 후 경매 정보 초기화
    public void initLiveItem(Long liveItemId) {
        LiveItem liveItem = liveItemRepository.findById(liveItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVEITEM_NOT_FOUND));

        String redisKey = "liveItem:" + liveItemId;

        Map<String, String> initialData = new HashMap<>();

        // 최초 입찰가는 시작가, 최초 입찰자는 없음.
        initialData.put("maxBidPrice", String.valueOf(liveItem.getInitPrice()));
        initialData.put("currentBidderId", "");

        bidRedisService.setHash(redisKey, initialData);
    }
}
