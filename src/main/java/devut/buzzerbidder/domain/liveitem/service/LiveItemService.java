package devut.buzzerbidder.domain.liveitem.service;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.service.AuctionRoomService;
import devut.buzzerbidder.domain.likelive.service.LikeLiveService;
import devut.buzzerbidder.domain.liveBid.service.LiveBidRedisService;
import devut.buzzerbidder.domain.liveitem.dto.request.LiveItemCreateRequest;
import devut.buzzerbidder.domain.liveitem.dto.request.LiveItemModifyRequest;
import devut.buzzerbidder.domain.liveitem.dto.request.LiveItemSearchRequest;
import devut.buzzerbidder.domain.liveitem.dto.response.ItemDto;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemCreateResponse;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemDetailResponse;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemListResponse;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemModifyResponse;
import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemResponse;
import devut.buzzerbidder.domain.liveitem.dto.response.RoomDto;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import devut.buzzerbidder.domain.liveitem.entity.LiveItemImage;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.image.ImageService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class LiveItemService {

    private final LiveItemRepository liveItemRepository;
    private final LikeLiveService likeLiveService;
    private final AuctionRoomService auctionRoomService;
    private final ImageService imageService;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;
    private final LiveBidRedisService  liveBidRedisService;

    public LiveItemCreateResponse writeLiveItem(LiveItemCreateRequest reqBody, User user) {

        LocalDateTime now = LocalDateTime.now();

        // 1. 현재 시간과 liveTime 차이 확인
        if (reqBody.startAt().isBefore(now.plusHours(1))) {
            throw new BusinessException(ErrorCode.CLOSE_LIVETIME);
        }

        LocalTime liveTimeOnly = reqBody.startAt().toLocalTime();
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

        // 4. 이미지 있는지 체크
        if (reqBody.images() == null || reqBody.images().isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_FILE_EMPTY);
        }

        String lockKey = "lock:auction-room:" + reqBody.startAt().truncatedTo(ChronoUnit.MINUTES);
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            // 락 획득 (최대 3초 대기)
            acquired = lock.tryLock(3, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException(ErrorCode.AUCTION_ROOM_BUSY);
            }
            // ===== 임계 영역: 트랜잭션 안에서 DB 작업 =====
            return transactionTemplate.execute(status -> {

                // 경매 시간 기반 방 할당
                AuctionRoom auctionRoom = auctionRoomService.assignRoom(reqBody.startAt(), reqBody.roomIndex());

                LiveItem liveItem = new LiveItem(reqBody, user);
                liveItemRepository.save(liveItem);
                auctionRoom.addItem(liveItem);

                reqBody.images().forEach(url ->
                    liveItem.addImage(new LiveItemImage(url, liveItem))
                );

                ItemDto itemDto =  new ItemDto(
                    liveItem.getId(),
                    liveItem.getName(),
                    liveItem.getImages().get(0).getImageUrl()
                );

                RoomDto roomDto = new RoomDto(
                    auctionRoom.getId(),
                    auctionRoom.getRoomIndex(),
                    auctionRoom.getLiveTime()
                );

                return new LiveItemCreateResponse(itemDto, roomDto);

            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AUCTION_ROOM_BUSY);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public LiveItemModifyResponse modifyLiveItem(Long id, LiveItemModifyRequest reqBody, User user) {

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

        // 이미지 있는지 체크
        if (reqBody.images() == null || reqBody.images().isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_FILE_EMPTY);
        }

        // 시간정보 획득
        LocalDateTime oldLiveTime = liveItem.getLiveTime();
        LocalDateTime newLiveTime = reqBody.startAt();
        boolean liveTimeChanged = !oldLiveTime.equals(newLiveTime);

        // 락 키 결정, 수정은 경매 시간이 바뀔 경우 양쪽 락을 획득해야함
        List<String> lockKeys = new ArrayList<>();
        lockKeys.add("lock:auction-room:" + oldLiveTime.truncatedTo(ChronoUnit.MINUTES));
        if (liveTimeChanged) {
            lockKeys.add("lock:auction-room:" + newLiveTime.truncatedTo(ChronoUnit.MINUTES));
        }

        // MultiLock 생성
        RLock[] locks = lockKeys.stream()
            .map(redissonClient::getLock)
            .toArray(RLock[]::new);
        RLock multiLock = redissonClient.getMultiLock(locks);

        boolean acquired = false;
        LiveItemModifyResponse response;

        try {
            // 모든 락을 한 번에 시도
            acquired = multiLock.tryLock(3, TimeUnit.SECONDS);
            if (!acquired)
                throw new BusinessException(ErrorCode.AUCTION_ROOM_BUSY);

            LiveItem currentLiveItem = liveItemRepository.findLiveItemWithImagesById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));


            // 기존 이미지와 바뀔 이미지 세팅
            List<String> oldImageUrls = currentLiveItem.getImages().stream()
                .map(LiveItemImage::getImageUrl)
                .toList();
            List<String> newImageUrls = reqBody.images();

            // 삭제할 이미지 : 기존에는 있었는데 새 목록에는 없는 것
            List<String> toDelete = oldImageUrls.stream()
                .filter(url -> !newImageUrls.contains(url))
                .toList();

            // 이 블록은 트랜젝션
            response = transactionTemplate.execute(status -> {

                // 일반정보 수정
                currentLiveItem.modifyLiveItem(reqBody);

                // 경매시간 변경 시
                if (liveTimeChanged) {

                    // 1. 1시간 안에 시작하는지 확인
                    if (reqBody.startAt().isBefore(now.plusHours(1))) {
                        throw new BusinessException(ErrorCode.INVALID_LIVETIME);
                    }

                    LocalTime liveTimeOnly = reqBody.startAt().toLocalTime();
                    // 2. 허용 시간 범위 체크 (09:00 ~ 23:00)
                    if (liveTimeOnly.isBefore(LocalTime.of(9, 0)) || liveTimeOnly.isAfter(
                        LocalTime.of(23, 0))) {
                        throw new BusinessException(ErrorCode.INVALID_LIVETIME);
                    }

                    // 3. 30분 단위 체크 + 초 체크
                    int minute = liveTimeOnly.getMinute();
                    int second = liveTimeOnly.getSecond();
                    if ((minute != 0 && minute != 30) || second != 0) {
                        throw new BusinessException(ErrorCode.INVALID_LIVETIME);
                    }

                    // 경매방 재할당
                    AuctionRoom oldRoom = currentLiveItem.getAuctionRoom();
                    oldRoom.removeItem(currentLiveItem);

                    AuctionRoom newRoom = auctionRoomService.assignRoom(reqBody.startAt(), reqBody.roomIndex());
                    currentLiveItem.changeAuctionRoom(newRoom);

                    newRoom.addItem(currentLiveItem);
                }

                // DB 이미지 목록 갱신
                currentLiveItem.deleteImageUrls();
                newImageUrls.forEach(url ->
                    currentLiveItem.addImage(new LiveItemImage(url, currentLiveItem)));

                liveItemRepository.save(currentLiveItem);

                ItemDto itemDto =  new ItemDto(
                    currentLiveItem.getId(),
                    currentLiveItem.getName(),
                    currentLiveItem.getImages().get(0).getImageUrl()
                );

                AuctionRoom roomToRes = currentLiveItem.getAuctionRoom();
                RoomDto roomDto = new RoomDto(
                    roomToRes.getId(),
                    roomToRes.getRoomIndex(),
                    roomToRes.getLiveTime()
                );

                return new LiveItemModifyResponse(itemDto, roomDto);

            });

            // S3에서 삭제
            if (!toDelete.isEmpty()) {
                imageService.deleteFiles(toDelete);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AUCTION_ROOM_BUSY);
        } finally {
            // 락 해제
            if (acquired) {
                multiLock.unlock();
            }
        }
        return response;
    }



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

        // 분산 락 설정 (해당 아이템이 속한 경매 시간대 기준)
        String lockKey = "lock:auction-room:" + liveItem.getLiveTime().truncatedTo(ChronoUnit.MINUTES);
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        List<String> imagesToDelete = new ArrayList<>();

        try {
            acquired = lock.tryLock(3, TimeUnit.SECONDS); // 워치독 활용
            if (!acquired) {
                throw new BusinessException(ErrorCode.AUCTION_ROOM_BUSY);
            }

            // 임계 영역: 트랜잭션 시작
            transactionTemplate.executeWithoutResult(status -> {
                LiveItem currentItem = liveItemRepository.findLiveItemWithImagesById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

                imagesToDelete.addAll(currentItem.getImages().stream()
                    .map(LiveItemImage::getImageUrl).toList());

                // 방에서 제거
                AuctionRoom auctionRoom = currentItem.getAuctionRoom();
                auctionRoom.removeItem(currentItem);

                // DB에서 삭제 (OrphanRemoval 설정에 따라 이미지도 함께 삭제됨)
                liveItemRepository.delete(currentItem);
            });

            // 트랜잭션 성공 후에만 S3 파일 삭제 (데이터 일관성)
            if (!imagesToDelete.isEmpty()) {
                imageService.deleteFiles(imagesToDelete);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AUCTION_ROOM_BUSY);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(readOnly = true)
    public LiveItemDetailResponse getLiveItem(Long id) {

        LiveItem liveItem = liveItemRepository.findLiveItemWithImagesById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        long likeCount = likeLiveService.countByLiveItemId(id);

        String redisKey = "liveItem:" + liveItem.getId();

        String currentMaxPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");
        Long currentMaxPrice = (currentMaxPriceStr != null) ? Integer.parseInt(currentMaxPriceStr) : liveItem.getInitPrice();

        return new LiveItemDetailResponse(
            liveItem.getId(),
            liveItem.getSellerUserId(),
            liveItem.getName(),
            liveItem.getCategory(),
            liveItem.getDescription(),
            liveItem.getDeliveryInclude(),
            liveItem.getItemStatus(),
            liveItem.getAuctionStatus(),
            liveItem.getLiveTime(),
            liveItem.getDirectDealAvailable(),
            liveItem.getRegion(),
            liveItem.getPreferredPlace(),
            liveItem.getImages().stream()
                .map(LiveItemImage::getImageUrl)
                .toList(),
            likeCount,
            currentMaxPrice
        );
    }

    // TODO: 레디스에서 현재 입찰가로 가격 필터링 로직 추가
    @Transactional(readOnly = true)
    public LiveItemListResponse  getLiveItems(
        LiveItemSearchRequest reqBody,
        Pageable pageable
    ) {

        Page<LiveItemResponse> page = liveItemRepository.searchLiveItems(reqBody.name(),reqBody.category(), pageable);

        // 2. Redis에서 현재 입찰가 가져오기
        List<LiveItemResponse> dtoList = page.getContent().stream()
            .map(item -> {
                String redisKey = "liveItem:" + item.id();
                String maxBidPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");

                Long currentMaxBidPrice = (maxBidPriceStr != null)
                    ? Long.parseLong(maxBidPriceStr)
                    : item.currentPrice();

                // DTO에 현재 입찰가 세팅
                return new LiveItemResponse(
                    item.id(),
                    item.name(),
                    item.image(),
                    item.liveTime(),
                    item.auctionStatus(),
                    currentMaxBidPrice
                );
            })
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


        List<LiveItemResponse> dtoList = items.stream()
            .map(item -> {
                String redisKey = "liveItem:" + item.getId();
                String maxBidPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");

                Long currentMaxBidPrice = (maxBidPriceStr != null)
                    ? Long.parseLong(maxBidPriceStr)
                    : item.getInitPrice(); // DB 값 fallback

                return new LiveItemResponse(
                    item.getId(),
                    item.getName(),
                    item.getImages().get(0).getImageUrl(),
                    item.getLiveTime(),
                    item.getAuctionStatus(),
                    currentMaxBidPrice
                );
            })
            .toList();

        return new LiveItemListResponse(dtoList, dtoList.size());

    }
}
