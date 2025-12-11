package devut.buzzerbidder.domain.liveitem.service;

import devut.buzzerbidder.domain.likelive.service.LikeLiveService;
import devut.buzzerbidder.domain.liveitem.dto.request.LiveItemCreateRequest;
import devut.buzzerbidder.domain.liveitem.dto.request.LiveItemModifyRequest;
import devut.buzzerbidder.domain.liveitem.dto.request.LiveItemSearchRequest;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemDetailResponse;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemListResponse;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemResponse;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import devut.buzzerbidder.domain.liveitem.entity.LiveItemImage;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LiveItemService {

    private final LiveItemRepository liveItemRepository;
    private final LikeLiveService likeLiveService;

    @Transactional
    public LiveItemResponse writeLiveItem(LiveItemCreateRequest reqBody, User user) {

        LiveItem liveItem = new LiveItem(reqBody, user);


        /* 이미지 처리코드 없어서 임시 주석처리
        if (reqBody.images() == null || reqBody.images().isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_FILE_EMPTY);
        }

        reqBody.images().forEach(url ->
            liveItem.addImage(new LiveItemImage(url, liveItem)));
        */

        // 임시 코드
        liveItem.addImage(new LiveItemImage("example.jpg", liveItem));

        liveItemRepository.save(liveItem);

        return new LiveItemResponse(liveItem);

    }

    @Transactional
    public LiveItemResponse modifyLiveItem(Long id, LiveItemModifyRequest reqBody, User user) {

        LiveItem liveItem = liveItemRepository.findLiveItemWithImagesById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        // 작성자 검증
        if (!liveItem.getSellerUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        liveItem.modifyLiveItem(reqBody);

        /* 이미지 처리 로직 없어서 임시 주석처리
        // 새 이미지 URL이 있고, 기존과 다를 때만 교체
        if (reqBody.images() != null) {
            List<String> oldImageUrls = liveItem.getImages().stream()
                .map(LiveItemImage::getImageUrl)
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
            liveItem.deleteImageUrls();
            newImageUrls.forEach(url ->
                liveItem.addImage(new LiveItemImage(url, liveItem)));
        }
         */

        liveItemRepository.save(liveItem);

        return new LiveItemResponse(liveItem);

    }

    @Transactional
    public void deleteLiveItem(Long id, User user) {

        LiveItem liveItem = liveItemRepository.findLiveItemWithImagesById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        if (!liveItem.getSellerUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        /* 이미지 처리 로직 없어서 임시 주석처리
        if (!liveItem.getImages().isEmpty()) {
            List<String> oldImageUrls = liveItem.getImages().stream()
                .map(LiveItemImage::getImageUrl)
                .toList();
            imageService.deleteFiles(oldImageUrls);
            liveItem.deleteImageUrls(); // 이미지 리스트 초기화
        }
        */


        liveItemRepository.delete(liveItem);
    }

    @Transactional(readOnly = true)
    public LiveItemDetailResponse getLiveItem(Long id) {

        LiveItem liveItem = liveItemRepository.findLiveItemWithImagesById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        long likeCount = likeLiveService.countByLiveItemId(id);

        // 현재가 보내는 로직 추가
        return new LiveItemDetailResponse(
            liveItem.getName(),
            liveItem.getCategory(),
            liveItem.getDescription(),
            liveItem.getDeliveryInclude(),
            liveItem.getItemstatus(),
            liveItem.getDirectDealAvailable(),
            liveItem.getRegion(),
            liveItem.getPreferredPlace(),
            liveItem.getImages().stream()
                .map(LiveItemImage::getImageUrl)
                .toList(),
            likeCount
        );
    }

    // TODO: 입찰가 확인추가하기
    @Transactional(readOnly = true)
    public LiveItemListResponse  getLiveItems(
        LiveItemSearchRequest reqBody,
        Pageable pageable
    ) {

        Page<LiveItem> page = liveItemRepository.searchLiveItems(reqBody.name(),reqBody.category(), pageable);

        // 이곳에 레디스 가격 필터링

        List<LiveItemResponse> dtoList =
            page.getContent().stream()
                .map(LiveItemResponse::new)
                .toList();

        return new LiveItemListResponse(dtoList, page.getTotalElements());
    }


    public void changeAuctionStatus(Long id, User user, AuctionStatus auctionStatus) {

        LiveItem liveItem = liveItemRepository.findLiveItemWithImagesById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        // 작성자 검증
        if (!liveItem.getSellerUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        liveItem.changeAuctionStatus(auctionStatus);

    }

    @Transactional(readOnly = true)
    public LiveItemListResponse getHotLiveItems(int limit) {

        Pageable pageable = PageRequest.of(0, limit);
        List<Long> ids = liveItemRepository.findHotLiveItems(pageable);

        List<LiveItem> items = liveItemRepository.findLiveItemsWithImages(ids);


        List<LiveItemResponse> dtoList =
            items.stream()
                .map(LiveItemResponse::new)
                .toList();

        return new LiveItemListResponse(dtoList, dtoList.size());

    }

    public Optional<LiveItem> findById(Long id) {
        return liveItemRepository.findById(id);
    }

}