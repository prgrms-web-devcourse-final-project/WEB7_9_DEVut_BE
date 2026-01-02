package devut.buzzerbidder.domain.user.service;

import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.repository.DelayedDealRepository;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.deal.enums.AuctionType;
import devut.buzzerbidder.domain.likedelayed.repository.LikeDelayedRepository;
import devut.buzzerbidder.domain.likelive.repository.LikeLiveRepository;
import devut.buzzerbidder.domain.user.dto.response.UserDealItemResponse;
import devut.buzzerbidder.domain.user.dto.response.UserDealListResponse;
import devut.buzzerbidder.domain.user.dto.response.UserDealResponse;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDealService {

    private final UserRepository userRepository;
    private final LiveDealRepository liveDealRepository;
    private final DelayedDealRepository delayedDealRepository;
    private final LikeLiveRepository likeLiveRepository;
    private final LikeDelayedRepository likeDelayedRepository;

    public UserDealListResponse getUserDeals(User user, Pageable pageable, String type) {
        // 전체 개수 계산
        long totalElements = userRepository.countMyDeals(user.getId(), type);
        
        // UNION 쿼리로 ID와 타입만 가져오기 (페이징 적용)
        List<Object[]> results = userRepository.findMyDealIdsAndTypes(
            user.getId(),
            type,
            pageable.getPageSize(),
            pageable.getOffset()
        );

        // 공통 로직으로 처리 (찜 여부 확인을 위해 user 전달)
        List<UserDealItemResponse> items = fetchAndMapDeals(results, user);

        return new UserDealListResponse(items, totalElements);
    }

    public UserDealResponse getUserDeal(User user, AuctionType type, Long dealId) {
        if (type == AuctionType.LIVE) {
            LiveDeal liveDeal = liveDealRepository.findByIdWithItemAndImages(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));
            
            // 구매자 검증
            if (!liveDeal.getBuyer().getId().equals(user.getId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
            }
            
            return UserDealResponse.fromLiveDeal(liveDeal);
        } else if (type == AuctionType.DELAYED) {
            DelayedDeal delayedDeal = delayedDealRepository.findByIdWithItemAndImages(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));
            
            // 구매자 검증
            if (!delayedDeal.getBuyer().getId().equals(user.getId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
            }
            
            return UserDealResponse.fromDelayedDeal(delayedDeal);
        } else {
            throw new BusinessException(ErrorCode.DEAL_INVALID_TYPE);
        }
    }

    /**
     * ID와 타입 리스트를 받아서 엔티티를 조회하고 DTO로 변환하는 공통 메서드
     * N+1 문제를 해결하기 위해 IN 절로 한 번에 조회
     * @param results ID와 타입 리스트
     * @param user 현재 사용자 (찜 여부 확인용)
     */
    private List<UserDealItemResponse> fetchAndMapDeals(List<Object[]> results, User user) {
        // ID와 타입 분리
        List<Long> liveDealIds = new ArrayList<>();
        List<Long> delayedDealIds = new ArrayList<>();
        
        for (Object[] row : results) {
            Long id = ((Number) row[0]).longValue();
            String type = (String) row[1];
            if ("LIVE".equals(type)) {
                liveDealIds.add(id);
            } else {
                delayedDealIds.add(id);
            }
        }

        // 엔티티 조회 (빈 리스트 체크 포함, N+1 방지를 위해 FETCH JOIN 사용)
        Map<Long, LiveDeal> liveDealMap = liveDealIds.isEmpty() 
            ? Collections.emptyMap()
            : liveDealRepository.findByIdsWithItemAndImages(liveDealIds).stream()
                .collect(Collectors.toMap(LiveDeal::getId, deal -> deal));

        Map<Long, DelayedDeal> delayedDealMap = delayedDealIds.isEmpty() 
            ? Collections.emptyMap()
            : delayedDealRepository.findByIdsWithItemAndImages(delayedDealIds).stream()
                .collect(Collectors.toMap(DelayedDeal::getId, deal -> deal));

        // 찜 여부 Batch 조회 (N+1 해결)
        Set<Long> likedLiveItemIds = new HashSet<>();
        Set<Long> likedDelayedItemIds = new HashSet<>();
        
        if (user != null) {
            // LiveItem 찜 여부 조회
            if (!liveDealMap.isEmpty()) {
                List<Long> liveItemIds = liveDealMap.values().stream()
                    .map(deal -> deal.getItem().getId())
                    .toList();
                if (!liveItemIds.isEmpty()) {
                    likedLiveItemIds = new HashSet<>(
                        likeLiveRepository.findLikedLiveItemIds(user.getId(), liveItemIds)
                    );
                }
            }
            
            // DelayedItem 찜 여부 조회
            if (!delayedDealMap.isEmpty()) {
                List<Long> delayedItemIds = delayedDealMap.values().stream()
                    .map(deal -> deal.getItem().getId())
                    .toList();
                if (!delayedItemIds.isEmpty()) {
                    likedDelayedItemIds = new HashSet<>(
                        likeDelayedRepository.findLikedDelayedItemIds(user.getId(), delayedItemIds)
                    );
                }
            }
        }

        // TODO: 판매자 이름 조회 (필요시 추가)

        // 최종 결과 조립 (순서 유지)
        List<UserDealItemResponse> items = new ArrayList<>();
        for (Object[] row : results) {
            Long id = ((Number) row[0]).longValue();
            String type = (String) row[1];

            if ("LIVE".equals(type)) {
                LiveDeal deal = liveDealMap.get(id);
                if (deal != null) {
                    Boolean wish = likedLiveItemIds.contains(deal.getItem().getId());
                    UserDealItemResponse response = UserDealItemResponse.fromLiveDeal(deal, wish);
                    // 판매자 이름 설정 (필요시)
                    // String sellerName = getSellerName(deal.getItem().getSellerUserId());
                    // response = response.withSellerName(sellerName);
                    items.add(response);
                }
            } else if ("DELAYED".equals(type)) {
                DelayedDeal deal = delayedDealMap.get(id);
                if (deal != null) {
                    Boolean wish = likedDelayedItemIds.contains(deal.getItem().getId());
                    UserDealItemResponse response = UserDealItemResponse.fromDelayedDeal(deal, wish);
                    // 판매자 이름 설정 (필요시)
                    // String sellerName = getSellerName(deal.getItem().getSellerUserId());
                    // response = response.withSellerName(sellerName);
                    items.add(response);
                }
            }
        }
        
        return items;
    }
}

