package devut.buzzerbidder.domain.auction.service;

import devut.buzzerbidder.domain.auction.dto.request.AuctionSearchRequest;
import devut.buzzerbidder.domain.auction.dto.response.AuctionListResponse;
import devut.buzzerbidder.domain.auction.dto.response.AuctionListResponse.AuctionItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.liveBid.service.LiveBidRedisService;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemResponse;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private final DelayedItemRepository delayedItemRepository;
    private final LiveItemRepository liveItemRepository;
    private final LiveBidRedisService liveBidRedisService;

    /**
     * 통합 경매 검색 (페이징, 필터링, 검색)
     */
    public AuctionListResponse searchAuctions(AuctionSearchRequest request, Pageable pageable) {
        List<AuctionItem> allAuctions = new ArrayList<>();

        // 타입별 조회
        if ("ALL".equals(request.type()) || "DELAYED".equals(request.type())) {
            List<DelayedItem> delayedItems = getDelayedItems(request);
            allAuctions.addAll(convertDelayedItems(delayedItems));
        }

        if ("ALL".equals(request.type()) || "LIVE".equals(request.type())) {
            List<LiveItem> liveItems = getLiveItems(request);
            allAuctions.addAll(convertLiveItems(liveItems));
        }

        // 정렬
        allAuctions.sort(Comparator.comparing(AuctionItem::endTime));

        // 페이징
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allAuctions.size());

        List<AuctionItem> pagedAuctions = start < allAuctions.size()
            ? allAuctions.subList(start, end)
            : List.of();

        return new AuctionListResponse(pagedAuctions, allAuctions.size());
    }

    private List<DelayedItem> getDelayedItems(AuctionSearchRequest request) {
        // 키워드 검색
        if (hasSearchFilters(request)) {
            Page<DelayedItem> page = delayedItemRepository.searchDelayedItems(
                request.keyword(),
                convertCategory(request.category()),
                request.minPrice(),
                request.maxPrice(),
                Pageable.unpaged()
            );

            return page.getContent().stream()
                .filter(item -> item.getAuctionStatus() == DelayedItem.AuctionStatus.IN_PROGRESS)
                .toList();
        } else {
            // 필터 없으면 단순 조회
            return delayedItemRepository
                .findByAuctionStatusWithImages(DelayedItem.AuctionStatus.IN_PROGRESS);
        }
    }

    private List<LiveItem> getLiveItems(AuctionSearchRequest request) {
        // 키워드 검색
        if (request.keyword() != null || request.category() != null) {
            Page<LiveItemResponse> page = liveItemRepository.searchLiveItems(
                request.keyword(),
                request.category(),
                Pageable.unpaged()
            );

            List<Long> ids = page.getContent().stream()
                .map(LiveItemResponse::id)
                .toList();

            if (ids.isEmpty()) return List.of();

            List<LiveItem> items = liveItemRepository.findLiveItemsWithImages(ids);

            return items.stream()
                .filter(item -> item.getAuctionStatus() == LiveItem.AuctionStatus.BEFORE_BIDDING ||
                    item.getAuctionStatus() == LiveItem.AuctionStatus.IN_PROGRESS)
                .toList();
        } else {
            // 필터 없으면 단순 조회
            return liveItemRepository.findByAuctionStatusInWithImages(
                List.of(LiveItem.AuctionStatus.BEFORE_BIDDING, LiveItem.AuctionStatus.IN_PROGRESS)
            );
        }
    }

    private List<AuctionItem> convertDelayedItems(List<DelayedItem> items) {
        return items.stream()
            .map(item -> new AuctionItem(
                item.getId(),
                "DELAYED",
                item.getName(),
                item.getImages().isEmpty() ? null : item.getImages().get(0).getImageUrl(),
                item.getCurrentPrice(),
                item.getEndTime(),
                item.getAuctionStatus().name(),
                null
            ))
            .toList();
    }

    private List<AuctionItem> convertLiveItems(List<LiveItem> items) {
        return items.stream()
            .filter(item -> item.getAuctionRoom() != null)
            .map(item -> new AuctionItem(
                item.getId(),
                "LIVE",
                item.getName(),
                item.getImages().isEmpty() ? null : item.getImages().get(0).getImageUrl(),
                item.getInitPrice(),
                calculateLiveItemEndTime(item),
                item.getAuctionStatus().name(),
                item.getAuctionRoom().getId()
            ))
            .toList();
    }

    private java.time.LocalDateTime calculateLiveItemEndTime(LiveItem item) {
        if (item.getAuctionStatus() == LiveItem.AuctionStatus.IN_PROGRESS) {
            // 진행 중: Redis에서 정확한 종료 시간 조회
            String redisKey = "liveItem:" + item.getId();
            String endTimeStr = liveBidRedisService.getHashField(redisKey, "endTime");

            if (endTimeStr != null) {
                long endTimeMillis = Long.parseLong(endTimeStr);
                return java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(endTimeMillis),
                    java.time.ZoneId.systemDefault()
                );
            }
        }
        // BEFORE_BIDDING 또는 Redis 조회 실패 : 순서 기반 예상 시간
        int ItemIndex = item.getAuctionRoom().getLiveItems().indexOf(item);
        return item.getAuctionRoom().getLiveTime()
            .plusMinutes((ItemIndex + 1) * 5);
    }

    private boolean hasSearchFilters(AuctionSearchRequest request) {
        return request.keyword() != null ||
            request.category() != null ||
            request.minPrice() != null ||
            request.maxPrice() != null;
    }

    private DelayedItem.Category convertCategory(LiveItem.Category category) {
        if (category == null) return null;
        return DelayedItem.Category.valueOf(category.name());
    }

}
