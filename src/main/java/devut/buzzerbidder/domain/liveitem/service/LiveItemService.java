package devut.buzzerbidder.domain.liveitem.service;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.service.AuctionRoomService;
import devut.buzzerbidder.domain.auctionroom.service.AuctionRoomStatePushService;
import devut.buzzerbidder.domain.deal.service.LiveDealService;
import devut.buzzerbidder.domain.likelive.repository.LikeLiveRepository;
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
import devut.buzzerbidder.domain.liveitem.event.LiveAuctionEndedEvent;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.image.ImageService;
import io.micrometer.core.annotation.Timed;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveItemService {

    private final LiveItemRepository liveItemRepository;
    private final LikeLiveService likeLiveService;
    private final AuctionRoomService auctionRoomService;
    private final LiveDealService liveDealService;
    private final WalletService walletService;
    private final UserService userService;
    private final ImageService imageService;
    private final AuctionRoomStatePushService auctionRoomStatePushService;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;
    private final LiveBidRedisService  liveBidRedisService;
    private final LikeLiveRepository likeLiveRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final LiveItemWebSocketService liveItemWebSocketService;
    private final RedisTemplate<String, String> redisTemplate;


    @Timed(
            value = "buzzerbidder.redis.liveitem",
            extraTags = {"op", "write"},
            histogram = true
    )
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

    @Timed(
            value = "buzzerbidder.redis.liveitem",
            extraTags = {"op", "modify"},
            histogram = true
    )
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

                    if(oldRoom.getLiveItems().isEmpty()){
                        auctionRoomService.deleteAuctionRoom(oldRoom);
                    }

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
                //imageService.deleteFiles(toDelete);
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
                if(auctionRoom.getLiveItems().isEmpty()){
                    auctionRoomService.deleteAuctionRoom(auctionRoom);
                }

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
    public LiveItemDetailResponse getLiveItem(Long id, Long userId) {

        LiveItem liveItem = liveItemRepository.findLiveItemWithImagesById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        boolean isLiked = false;
        if (userId != null) {
            isLiked = likeLiveRepository.existsByUserIdAndLiveItemId(userId, liveItem.getId());
            // 또는 existsByUser_IdAndLiveItem_Id(userId, liveItem.getId());
        }
        long likeCount = likeLiveService.countByLiveItemId(id);

        String redisKey = "liveItem:" + liveItem.getId();

        String currentMaxPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");
        Long currentMaxPrice = (currentMaxPriceStr != null) ? Integer.parseInt(currentMaxPriceStr) : liveItem.getCurrentPrice();

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
                liveItem.getInitPrice(),
                currentMaxPrice,
                isLiked,
                liveItem.getAuctionRoom().getId()
        );
    }

    @Transactional(readOnly = true)
    public LiveItemListResponse getLiveItems(
            LiveItemSearchRequest reqBody,
            Pageable pageable,
            Long userId
    ) {

        Long min = reqBody.minBidPrice();
        Long max = reqBody.maxBidPrice();

        Page<LiveItemResponse> page;

        // 가격필터 없는 경우: 기존 로직 그대로
        if (min == null && max == null) {
            page = liveItemRepository.searchLiveItems(
                reqBody.name(),
                reqBody.category(),
                reqBody.isSelling(),
                pageable
            );
        } else {
            // 가격 필터가 있는 경우

            long lo = (min != null) ? min : Long.MIN_VALUE;
            long hi = (max != null) ? max : Long.MAX_VALUE;

            // 1) Redis(ZSET): 입찰 있는 아이템 중 현재가 범위 통과 id 리스트
            List<Long> a = liveBidRedisService.zRangeByScoreAsLong("liveItems:currentPrice", lo, hi);

            // 2) DB(initPrice): 기본필터 + initPrice 범위 통과 후보
            List<Long> b = liveItemRepository.findIdsByInitPriceRangeWithBaseFilters(
                reqBody.name(),
                reqBody.category(),
                reqBody.isSelling(),
                min,
                max
            );

            // 3) b 중에서 hasBid=true 제거 (입찰 있는 애는 initPrice로 판단하면 안 됨)
            List<Boolean> hasBidFlags = liveBidRedisService.sIsMemberBatch("liveItems:hasBid", b);

            Set<Long> candidateIds = new HashSet<>(a);
            for (int i = 0; i < b.size(); i++) {
                if (!hasBidFlags.get(i)) {
                    candidateIds.add(b.get(i));
                }
            }

            if (candidateIds.isEmpty()) {
                return new LiveItemListResponse(List.of(), 0);
            }

            // 4) candidateIds로 DB에서 정확한 페이징/정렬 + 기존 필터 재적용
            page = liveItemRepository.searchLiveItemsWithinIds(
                candidateIds,
                reqBody.name(),
                reqBody.category(),
                reqBody.isSelling(),
                pageable
            );
        }

        List<LiveItemResponse> baseList = page.getContent();

        List<Long> liveItemIds = baseList.stream().map(LiveItemResponse::id).toList();

        Set<Long> likedSet = Collections.emptySet();
        if (userId != null && !liveItemIds.isEmpty()) {
            likedSet = new HashSet<>(likeLiveRepository.findLikedLiveItemIds(userId, liveItemIds));
        }

        final Set<Long> finalLikedSet = likedSet;

        // 2. Redis에서 현재 입찰가 가져오기 + 찜 여부 넣기
        List<LiveItemResponse> dtoList = page.getContent().stream()
                .map(item -> {
                    String redisKey = "liveItem:" + item.id();
                    String maxBidPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");

                    Long currentMaxBidPrice = (maxBidPriceStr != null)
                            ? Long.parseLong(maxBidPriceStr)
                            : item.currentPrice();

                    boolean isLiked = finalLikedSet.contains(item.id());

                    // DTO에 현재 입찰가 세팅
                    return new LiveItemResponse(
                            item.id(),
                            item.name(),
                            item.image(),
                            item.startAt(),
                            item.auctionStatus(),
                            item.initPrice(),
                            currentMaxBidPrice,
                            isLiked
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
    public LiveItemListResponse getHotLiveItems(
        int limit,
        Long userId) {

        Pageable pageable = PageRequest.of(0, limit);
        List<LiveItemResponse> beforeBidPrice = liveItemRepository.findHotLiveItems(pageable);

        List<Long> liveItemIds = beforeBidPrice.stream().map(LiveItemResponse::id).toList();

        Set<Long> likedSet = Collections.emptySet();
        if (userId != null && !liveItemIds.isEmpty()) {
            likedSet = new HashSet<>(likeLiveRepository.findLikedLiveItemIds(userId, liveItemIds));
        }

        final Set<Long> finalLikedSet = likedSet;

        List<LiveItemResponse> dtoList = beforeBidPrice.stream()
                .map(item -> {
                    String redisKey = "liveItem:" + item.id();
                    String maxBidPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");

                    Long currentMaxBidPrice = (maxBidPriceStr != null)
                            ? Long.parseLong(maxBidPriceStr)
                            : item.currentPrice(); // DB 값 fallback

                    boolean isLiked = finalLikedSet.contains(item.id());


                    return new LiveItemResponse(
                            item.id(),
                            item.name(),
                            item.image(),
                            item.startAt(),
                            item.auctionStatus(),
                            item.initPrice(),
                            currentMaxBidPrice,
                            isLiked
                    );
                })
                .toList();

        return new LiveItemListResponse(dtoList, dtoList.size());

    }

    @Transactional
    public void startAuction(Long itemId) {
        LiveItem liveItem = liveItemRepository.findByIdWithLock(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVEITEM_NOT_FOUND));

        // 이미 시작했으면 무시
        if (liveItem.getAuctionStatus() != AuctionStatus.BEFORE_BIDDING) {
            return;
        }

        // 경매방이 LIVE가 아니면 시작 안 함
        if (liveItem.getAuctionRoom().getAuctionStatus() != AuctionRoom.AuctionStatus.LIVE) {
            return;
        }

        // DB 상태 우선 변경 및 확정
        liveItem.changeAuctionStatus(AuctionStatus.IN_PROGRESS);
        liveItemRepository.saveAndFlush(liveItem); // 커밋 전 DB 반영 확인

        // 4. Redis 초기화
        try {
            initLiveItem(liveItem);

            liveItemWebSocketService.broadcastAuctionStart(
                liveItem.getAuctionRoom().getId(),
                liveItem.getId(),
                liveItem.getName(),
                liveItem.getInitPrice().intValue()
            );
        } catch (Exception e) {
            // Redis 실패 시 강제 예외 발생 -> DB 롤백 유도
            log.error("Redis 초기화 실패. Transaction Rollback. ItemId: {}", itemId, e);
            throw new BusinessException(ErrorCode.LIVEBID_INITIALIZATION_FAILED);
        }


        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                auctionRoomStatePushService.pushRefresh(liveItem.getAuctionRoom().getId(), "경매 시작");
            }
        });
    }

    @Transactional
    public void endAuction(Long itemId) {
        LiveItem liveItem = liveItemRepository.findByIdWithLock(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVEITEM_NOT_FOUND));

        if (liveItem.getAuctionStatus() != AuctionStatus.IN_PROGRESS) {
            log.warn("이미 종료되었거나 진행 중이 아닌 경매입니다. Item ID: {}", itemId);
            return;
        }

        String redisKey = "liveItem:" + itemId;

        // ending ZSET pop 레이스 방지, Redis endTime을 재검증해서 아직 시간이 남았으면 종료하지 않음
        String endTimeStr = liveBidRedisService.getHashField(redisKey, "endTime");
        if (endTimeStr != null && !endTimeStr.isBlank()) {
            long endTimeMs = Long.parseLong(endTimeStr);
            long nowMs = liveBidRedisService.getRedisNowMs();

            if (nowMs < endTimeMs) {
                // 아직 종료 시각이 안 됐는데 스케줄러가 먼저 pop 해버린 케이스 → 다시 등록하고 종료하지 않음
                liveBidRedisService.upsertEndingZset(itemId, endTimeMs);
                return;
            }
        }

        String currentBidderIdStr = liveBidRedisService.getHashField(redisKey, "currentBidderId");
        String maxBidPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");

        if (currentBidderIdStr == null || currentBidderIdStr.isEmpty()) { // 유찰
            liveItem.changeAuctionStatus(AuctionStatus.FAILED);
            log.info("경매 유찰 처리 완료 - Item ID: {}", itemId);
        } else { // 낙찰
            liveItem.changeAuctionStatus(AuctionStatus.PAYMENT_PENDING);
            log.info("경매 낙찰 - Item ID: {}, winnerId={}, price={}",
                    itemId, currentBidderIdStr, maxBidPriceStr);

            Long currentBidderId = Long.parseLong(currentBidderIdStr);
            Long maxBidPrice = Long.parseLong(maxBidPriceStr);

            liveItem.setCurrentPrice(maxBidPrice);

            Long winnerDeposit = null;
            if (currentBidderIdStr != null && !currentBidderIdStr.isBlank()) {
                String depositsKey = "liveItem:" + itemId + ":deposits";
                Object depObj = redisTemplate.opsForHash().get(depositsKey, currentBidderIdStr);

                if (depObj != null) {
                    winnerDeposit = Long.parseLong(depObj.toString());
                }
            }

            User fromUser = userService.findById(currentBidderId);
            User toUser = userService.findById(liveItem.getSellerUserId());
            walletService.transferBizz(fromUser, toUser, winnerDeposit);

            liveDealService.createDeal(itemId, currentBidderId, maxBidPrice, winnerDeposit);
        }

        boolean success = currentBidderIdStr != null && !currentBidderIdStr.isEmpty();
        Long winnerId = success ? Long.parseLong(currentBidderIdStr) : null;
        Integer finalPrice = success ? Integer.parseInt(maxBidPriceStr) : liveItem.getInitPrice().intValue();

        eventPublisher.publishEvent(
            new LiveAuctionEndedEvent(
                liveItem.getId(),
                liveItem.getName(),
                liveItem.getSellerUserId(),
                success,
                winnerId,
                finalPrice
            )
        );

        liveItemWebSocketService.broadcastAuctionEnd(
            liveItem.getAuctionRoom().getId(),
            liveItem.getId(),
            liveItem.getName(),
            success,
            winnerId,
            finalPrice
        );

        Long nextItemId = liveItemRepository
                .findNextItemIds(liveItem.getAuctionRoom().getId(), liveItem.getId(), PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);


        // 마지막 아이템이면 AuctionRoom을 ENDED로 변경
        AuctionRoom room = liveItem.getAuctionRoom();
        if (nextItemId == null) {
            // endLive()는 LIVE 상태에서만 가능하니 멱등 방어
            if (room.getAuctionStatus() == AuctionRoom.AuctionStatus.LIVE) {
                room.endLive(); // AuctionRoom.auctionStatus = ENDED
            }
        }

        long nextStartAtMs = liveBidRedisService.getRedisNowMs() + 10_000L;

        // 커밋 성공한 경우에만 Redis 작업
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (nextItemId != null) {
                    liveBidRedisService.upsertStartingZset(nextItemId, nextStartAtMs);
                }

                auctionRoomStatePushService.pushRefresh(room.getId(), "낙찰/유찰 발생");

                // 정산 후에 키 삭제해야 depositsKey가 먼저 사라지는 사고를 방지
                liveBidRedisService.deleteLiveItemRedisKeys(itemId);
            }
        });
    }

    /**
     * 실시간 입찰 정보 redis에 최초 캐시
     * @param liveItem 라이브 경매품 엔티티
     */
    public void initLiveItem(LiveItem liveItem) {
        String redisKey = "liveItem:" + liveItem.getId();

        Map<String, String> initData = new HashMap<>();

        // 종료 시간 설정 (경매 시작 시간 + 40초) luaScript에서 읽을 수 있도록 UNIX Timestamp로 변환
        long endTime = liveBidRedisService.getRedisNowMs() + 10_000L; //TODO: 테스트 끝나면 40초로 변경

        // 초기화
        initData.put("maxBidPrice", String.valueOf(liveItem.getInitPrice()));
        initData.put("currentBidderId", "");
        initData.put("endTime", String.valueOf(endTime));

        liveBidRedisService.setHash(redisKey, initData);
        redisTemplate.expire(redisKey, Duration.ofMinutes(30));

        liveBidRedisService.upsertEndingZset(liveItem.getId(), endTime);
    }

}
