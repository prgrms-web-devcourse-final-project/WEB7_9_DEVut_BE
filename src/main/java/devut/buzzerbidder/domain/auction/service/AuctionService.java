package devut.buzzerbidder.domain.auction.service;

import devut.buzzerbidder.domain.auction.dto.request.AuctionSearchRequest;
import devut.buzzerbidder.domain.auction.dto.response.AuctionListResponse;
import devut.buzzerbidder.domain.auction.dto.response.AuctionListResponse.AuctionItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.likedelayed.service.LikeDelayedService;
import devut.buzzerbidder.domain.likelive.service.LikeLiveService;
import devut.buzzerbidder.domain.liveBid.service.LiveBidRedisService;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemResponse;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
    private final LikeDelayedService likeDelayedService;
    private final LikeLiveService likeLiveService;

    /**
     * 통합 경매 검색 (페이징, 필터링, 검색)
     */
    public AuctionListResponse searchAuctions(
        AuctionSearchRequest request,
        Pageable pageable,
        Long userId) {
        List<AuctionItem> allAuctions = new ArrayList<>();

        // 타입별 조회
        if ("ALL".equals(request.type()) || "DELAYED".equals(request.type())) {
            List<DelayedItem> delayedItems = getDelayedItems(request);
            allAuctions.addAll(convertDelayedItems(delayedItems, userId));
        }

        if ("ALL".equals(request.type()) || "LIVE".equals(request.type())) {
            List<LiveItem> liveItems = getLiveItems(request);
            allAuctions.addAll(convertLiveItems(liveItems, userId));
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
        // 키워드 검색 또는 isSelling=false (모든 상태 조회)
        if (hasSearchFilters(request)) {
            Page<DelayedItem> page = delayedItemRepository.searchDelayedItems(
                request.keyword(),
                convertCategory(request.category()),
                request.minPrice(),
                request.maxPrice(),
                request.isSelling(),
                DelayedItem.AuctionStatus.ACTIVE_STATUSES,
                Pageable.unpaged()
            );

            return page.getContent();
        } else {
            // 필터 없으면 단순 조회 (진행중만)
            return delayedItemRepository.findByAuctionStatusWithImages(
                List.of(DelayedItem.AuctionStatus.BEFORE_BIDDING, DelayedItem.AuctionStatus.IN_PROGRESS)
            );
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

            return liveItemRepository.findLiveItemsWithAuctionRoom(ids);
        } else {
            // 필터 없으면 단순 조회 (진행중만)
            return liveItemRepository.findByAuctionStatusInWithImages(
                List.of(LiveItem.AuctionStatus.BEFORE_BIDDING, LiveItem.AuctionStatus.IN_PROGRESS)
            );
        }
    }

    private List<AuctionItem> convertDelayedItems(List<DelayedItem> items, Long userId) {
        if (items.isEmpty()) return List.of();

        Set<Long> likedItemIds = userId != null
           ? likeDelayedService.findLikeDelayedItemIdsByUserId(userId)
           : Set.of();

        return items.stream()
            .map(item -> new AuctionItem(
                item.getId(),
                "DELAYED",
                item.getName(),
                item.getImages().isEmpty() ? null : item.getImages().get(0).getImageUrl(),
                item.getCurrentPrice(),
                item.getEndTime(),
                item.getAuctionStatus().name(),
                null,
                likedItemIds.contains(item.getId())
            ))
            .toList();
    }

    private List<AuctionItem> convertLiveItems(List<LiveItem> items, Long userId) {
        if (items.isEmpty()) return List.of();

        Set<Long> likedItemIds = userId != null
            ? likeLiveService.findLikedLiveItemIdsByUserId(userId)
            : Set.of();

        return items.stream()
            .map(item -> {
                String redisKey = "liveItem:" + item.getId();
                String maxBidPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");
                Long currentMaxBidPrice = (maxBidPriceStr != null)
                    ? Long.parseLong(maxBidPriceStr)
                    : item.getInitPrice();

                return new AuctionItem(
                    item.getId(),
                    "LIVE",
                    item.getName(),
                    item.getThumbnail(),
                    currentMaxBidPrice,
                    item.getAuctionRoom().getLiveTime(),
                    item.getAuctionStatus().name(),
                    item.getAuctionRoom().getId(),
                    likedItemIds.contains(item.getId())
                );
            })
            .toList();
    }

    private boolean hasSearchFilters(AuctionSearchRequest request) {
        return request.keyword() != null ||
            request.category() != null ||
            request.minPrice() != null ||
            request.maxPrice() != null ||
            Boolean.FALSE.equals(request.isSelling());
    }

    private DelayedItem.Category convertCategory(LiveItem.Category category) {
        if (category == null) return null;
        return DelayedItem.Category.valueOf(category.name());
    }

}
