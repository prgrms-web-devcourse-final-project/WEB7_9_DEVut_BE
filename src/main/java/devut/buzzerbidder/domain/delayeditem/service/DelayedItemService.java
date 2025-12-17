package devut.buzzerbidder.domain.delayeditem.service;

import devut.buzzerbidder.domain.delayedbid.repository.DelayedBidRepository;
import devut.buzzerbidder.domain.delayeditem.dto.request.DelayedItemCreateRequest;
import devut.buzzerbidder.domain.delayeditem.dto.request.DelayedItemModifyRequest;
import devut.buzzerbidder.domain.delayeditem.dto.request.DelayedItemSearchRequest;
import devut.buzzerbidder.domain.delayeditem.dto.response.DelayedItemDetailResponse;
import devut.buzzerbidder.domain.delayeditem.dto.response.DelayedItemListResponse;
import devut.buzzerbidder.domain.delayeditem.dto.response.DelayedItemResponse;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.AuctionStatus;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItemImage;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.likedelayed.service.LikeDelayedService;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.image.ImageService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DelayedItemService {

    private final DelayedItemRepository delayedItemRepository;
    private final LikeDelayedService likeDelayedService;
    private final DelayedBidRepository delayedBidRepository;
    private final ImageService imageService;

    @Transactional
    public DelayedItemResponse writeDelayedItem(DelayedItemCreateRequest reqBody, User user) {

        LocalDateTime now = LocalDateTime.now();

        // 종료 시간 검증 - 최소 3일 이후, 최대 10일 이내
        if (reqBody.endTime().isBefore(now.plusDays(3)) ||
            reqBody.endTime().isAfter(now.plusDays(10))) {
            throw new BusinessException(ErrorCode.INVALID_END_TIME);
        }

        DelayedItem delayedItem = new DelayedItem(reqBody, user);

        delayedItemRepository.save(delayedItem);

        if (reqBody.images() == null || reqBody.images().isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_FILE_EMPTY);
        }

        reqBody.images().forEach(url ->
            delayedItem.addImage(new DelayedItemImage(url, delayedItem)));

        return new DelayedItemResponse(delayedItem);
    }

    @Transactional
    public DelayedItemResponse modifyDelayedItem(Long id, DelayedItemModifyRequest reqBody, User user) {

        DelayedItem delayedItem = delayedItemRepository.findDelayedItemWithImagesById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        // 작성자 검증
        if (!delayedItem.getSellerUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 입찰 여부 검증 - 입찰이 있으면 수정 불가
        boolean hasBids = delayedBidRepository.existsByDelayedItem(delayedItem);
        if (hasBids) {
            throw new BusinessException(ErrorCode.EDIT_UNAVAILABLE_DUE_TO_BIDS);
        }

        // 경매 종료된 경우 수정 불가
        if (delayedItem.isAuctionEnded()) {
            throw new BusinessException(ErrorCode.AUCTION_ALREADY_ENDED);
        }

        // 일반 정보 수정
        delayedItem.modifyDelayedItem(reqBody);

        // 종료 시간 검증 - 최소 3일 이후, 최대 10일 이내
        LocalDateTime now = LocalDateTime.now();
        if (reqBody.endTime().isBefore(now.plusDays(3)) ||
            reqBody.endTime().isAfter(now.plusDays(10))) {
            throw new BusinessException(ErrorCode.INVALID_END_TIME);
        }

        // 새 이미지 URL이 있고, 기존과 다를 때만 교체
        if (reqBody.images() != null) {
            List<String> oldImageUrls = delayedItem.getImages().stream()
                .map(DelayedItemImage::getImageUrl)
                .toList();
            List<String> newImageUrls = reqBody.images();

            // 삭제할 이미지 : 기존에는 있었는데 새 목록에는 없는 것
            List<String> toDelete = oldImageUrls.stream()
                .filter(url -> !newImageUrls.contains(url))
                .toList();

            // S3에서 삭제
            if (!toDelete.isEmpty()) {
                imageService.deleteFiles(toDelete);
            }

            // DB 이미지 목록 갱신
            delayedItem.deleteImageUrls();
            newImageUrls.forEach(url ->
                delayedItem.addImage(new DelayedItemImage(url, delayedItem)));
        }

        delayedItemRepository.save(delayedItem);

        return new DelayedItemResponse(delayedItem);
    }

    @Transactional
    public void deleteDelayedItem(Long id, User user) {

        DelayedItem delayedItem = delayedItemRepository.findDelayedItemWithImagesById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        // 작성자 검증
        if (!delayedItem.getSellerUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 입찰 여부 확인 - 입찰이 있으면 삭제 불가
        boolean hasBids = delayedBidRepository.existsByDelayedItem(delayedItem);
        if (hasBids) {
            throw new BusinessException(ErrorCode.DELETE_UNAVAILABLE_DUE_TO_BIDS);
        }

        if (!delayedItem.getImages().isEmpty()) {
            List<String> oldImageUrls = delayedItem.getImages().stream()
                .map(DelayedItemImage::getImageUrl)
                .toList();
            imageService.deleteFiles(oldImageUrls);
            delayedItem.deleteImageUrls();
        }

        delayedItemRepository.delete(delayedItem);
    }

    @Transactional(readOnly = true)
    public DelayedItemDetailResponse getDelayedItem(Long id) {

        DelayedItem delayedItem = delayedItemRepository.findDelayedItemWithImagesById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        long likeCount = likeDelayedService.countByDelayedItemId(id);

        return new DelayedItemDetailResponse(
            delayedItem.getName(),
            delayedItem.getCategory(),
            delayedItem.getDescription(),
            delayedItem.getItemStatus(),
            delayedItem.getAuctionStatus(),
            delayedItem.getStartPrice(),
            delayedItem.getCurrentPrice(),
            delayedItem.getEndTime(),
            delayedItem.getDeliveryInclude(),
            delayedItem.getDirectDealAvailable(),
            delayedItem.getRegion(),
            delayedItem.getPreferredPlace(),
            delayedItem.getImages().stream()
                .map(DelayedItemImage::getImageUrl)
                .toList(),
            delayedItem.getSellerUserId(),
            likeCount
        );
    }

    @Transactional(readOnly = true)
    public DelayedItemListResponse getDelayedItems(
        DelayedItemSearchRequest reqBody,
        Pageable pageable
    ) {
        Page<DelayedItem> page = delayedItemRepository.searchDelayedItems(
            reqBody.name(),
            reqBody.category(),
            reqBody.minCurrentPrice(),
            reqBody.maxCurrentPrice(),
            pageable
        );

        List<DelayedItemResponse> dtoList =
            page.getContent().stream()
                .map(DelayedItemResponse::new)
                .toList();

        return new DelayedItemListResponse(dtoList, page.getTotalElements());
    }

    @Transactional
    public void changeAuctionStatus(Long id, User user, AuctionStatus auctionStatus) {

        DelayedItem delayedItem = delayedItemRepository.findDelayedItemWithImagesById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        // 작성자 검증
        if (!delayedItem.getSellerUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        delayedItem.changeAuctionStatus(auctionStatus);
    }

    @Transactional(readOnly = true)
    public DelayedItemListResponse getHotDelayedItems(int limit) {

        Pageable pageable = PageRequest.of(0, limit);
        List<Long> ids = delayedItemRepository.findHotDelayedItems(pageable);

        List<DelayedItem> items = delayedItemRepository.findDelayedItemsWithImages(ids);

        List<DelayedItemResponse> dtoList =
            items.stream()
                .map(DelayedItemResponse::new)
                .toList();

        return new DelayedItemListResponse(dtoList, dtoList.size());
    }
}
