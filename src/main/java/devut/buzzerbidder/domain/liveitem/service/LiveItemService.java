package devut.buzzerbidder.domain.liveitem.service;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.service.AuctionRoomService;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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
    private final AuctionRoomService auctionRoomService;

    //TODO: 이미지 처리 코드 활성화
    @Transactional
    public LiveItemResponse writeLiveItem(LiveItemCreateRequest reqBody, User user) {

        LocalDateTime now = LocalDateTime.now();

        // 1. 현재 시간과 liveTime 차이 확인
        if (reqBody.liveTime().isBefore(now.plusHours(1))) {
            throw new BusinessException(ErrorCode.CLOSE_LIVETIME);
        }

        LocalTime liveTimeOnly = reqBody.liveTime().toLocalTime();
        // 2. 허용 시간 범위 체크 (09:00 ~ 23:00)
        if (liveTimeOnly.isBefore(LocalTime.of(9, 0)) || liveTimeOnly.isAfter(LocalTime.of(23, 0))) {
            throw new BusinessException(ErrorCode.INVALID_LIVETIME);
        }

        // 3. 30분 단위 체크 + 초 체크
        int minute = liveTimeOnly.getMinute();
        int second = liveTimeOnly.getSecond();
        if ((minute != 0 && minute != 30)|| second !=0) {
            throw new BusinessException(ErrorCode.INVALID_LIVETIME);
        }

        // 경매 시간 기반 방 할당
        AuctionRoom auctionRoom = auctionRoomService.assignRoom(reqBody.liveTime());

        LiveItem liveItem = new LiveItem(reqBody, user);

        liveItemRepository.save(liveItem);

        auctionRoom.addItem(liveItem);

        /*
        if (reqBody.images() == null || reqBody.images().isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_FILE_EMPTY);
        }

        reqBody.images().forEach(url ->
            liveItem.addImage(new LiveItemImage(url, liveItem)));
        */

        // 임시 코드
        liveItem.addImage(new LiveItemImage("example.jpg", liveItem));

        return new LiveItemResponse(liveItem);

    }

    //TODO: 이미지 처리 코드 활성화
    @Transactional
    public LiveItemResponse modifyLiveItem(Long id, LiveItemModifyRequest reqBody, User user) {

        LiveItem liveItem = liveItemRepository.findLiveItemWithImagesById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        // 작성자 검증
        if (!liveItem.getSellerUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        LocalDateTime now = LocalDateTime.now();
        // 1. 현재 시간과 liveTime 차이 확인
        if (liveItem.getLiveTime().isBefore(now.plusHours(1))) {
            throw new BusinessException(ErrorCode.EDIT_UNAVAILABLE);
        }

        // 일반정보 수정
        LocalDateTime oldLiveTime = liveItem.getLiveTime();
        liveItem.modifyLiveItem(reqBody);

        // 경매시각이 바뀐 경우 유효성 검사 및 방 재할당
        boolean liveTimeChanged = !oldLiveTime.equals(reqBody.liveTime());
        if (liveTimeChanged) {

            // 1. 현재 시간과 liveTime 차이 확인
            if (reqBody.liveTime().isBefore(now.plusHours(1))) {
                throw new BusinessException(ErrorCode.INVALID_LIVETIME);
            }

            LocalTime liveTimeOnly = reqBody.liveTime().toLocalTime();
            // 2. 허용 시간 범위 체크 (09:00 ~ 23:00)
            if (liveTimeOnly.isBefore(LocalTime.of(9, 0)) || liveTimeOnly.isAfter(LocalTime.of(23, 0))) {
                throw new BusinessException(ErrorCode.INVALID_LIVETIME);
            }

            // 3. 30분 단위 체크 + 초 체크
            int minute = liveTimeOnly.getMinute();
            int second = liveTimeOnly.getSecond();
            if ((minute != 0 && minute != 30)|| second !=0) {
                throw new BusinessException(ErrorCode.INVALID_LIVETIME);
            }

            AuctionRoom oldRoom = liveItem.getAuctionRoom();
            oldRoom.removeItem(liveItem);

            AuctionRoom newRoom = auctionRoomService.assignRoom(reqBody.liveTime());
            liveItem.changeAuctionRoom(newRoom);

            newRoom.addItem(liveItem);
        }

        /*
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

    //TODO: 이미지 처리 코드 활성화
    @Transactional
    public void deleteLiveItem(Long id, User user) {

        LiveItem liveItem = liveItemRepository.findLiveItemWithImagesById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        // 작성자 검증
        if (!liveItem.getSellerUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        LocalDateTime now = LocalDateTime.now();
        // 1. 현재 시간과 liveTime 차이 확인
        if (liveItem.getLiveTime().isBefore(now.plusHours(1))) {
            throw new BusinessException(ErrorCode.EDIT_UNAVAILABLE);
        }

        AuctionRoom auctionRoom = liveItem.getAuctionRoom();
        auctionRoom.removeItem(liveItem);

        /*
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

    //TODO: 레디스에서 현재 입찰가 찾아서 추가하는 로직
    @Transactional(readOnly = true)
    public LiveItemDetailResponse getLiveItem(Long id) {

        LiveItem liveItem = liveItemRepository.findLiveItemWithImagesById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        long likeCount = likeLiveService.countByLiveItemId(id);

        // 현재 입찰가 보내는 로직 추가
        return new LiveItemDetailResponse(
            liveItem.getName(),
            liveItem.getCategory(),
            liveItem.getDescription(),
            liveItem.getDeliveryInclude(),
            liveItem.getItemStatus(),
            liveItem.getDirectDealAvailable(),
            liveItem.getRegion(),
            liveItem.getPreferredPlace(),
            liveItem.getImages().stream()
                .map(LiveItemImage::getImageUrl)
                .toList(),
            likeCount
        );
    }

    // TODO: 레디스에서 현재 입찰가로 가격 필터링 로직 추가
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


    @Transactional
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

}