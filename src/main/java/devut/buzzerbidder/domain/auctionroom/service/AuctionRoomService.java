package devut.buzzerbidder.domain.auctionroom.service;

import static reactor.netty.http.HttpConnectionLiveness.log;

import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionDaysDto;
import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionRoomDto;
import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionRoomItemDto;
import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionRoomListResponse;
import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionRoomResponse;
import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionRoomSlotDto;
import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionScheduleResponse;
import devut.buzzerbidder.domain.auctionroom.dto.response.LiveItemDto;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.AuctionStatus;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.RoomStatus;
import devut.buzzerbidder.domain.auctionroom.entity.RoomCountByStartAtRow;
import devut.buzzerbidder.domain.auctionroom.event.AuctionRoomStartedEvent;
import devut.buzzerbidder.domain.auctionroom.repository.AuctionRoomRepository;
import devut.buzzerbidder.domain.likelive.repository.LikeLiveRepository;
import devut.buzzerbidder.domain.liveBid.service.LiveBidRedisService;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItemImage;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionRoomService {

    private final AuctionRoomRepository auctionRoomRepository;
    private final LiveBidRedisService liveBidRedisService;
    private final ApplicationEventPublisher eventPublisher;
    private final LiveItemRepository liveItemRepository;
    private final LikeLiveRepository likeLiveRepository;

    public AuctionRoom assignRoom(LocalDateTime liveTime, long roomIndex) {

        List<AuctionRoom> rooms = auctionRoomRepository.findAllByLiveTime(liveTime);

        AuctionRoom targetRoom = null;


        // 순회하면서 OPEN 있는지 검사 + roomIndex로 방찾기
        for (AuctionRoom room : rooms) {

            if (room.getRoomIndex() == (roomIndex)) {
                targetRoom = room;
            }
        }

        // roomIndex 해당 경매방을 할당 가능한지 확인
        if (targetRoom != null) {

            if(targetRoom.getRoomStatus() == RoomStatus.OPEN) {
                return targetRoom;
            }

            if(targetRoom.getRoomStatus() == RoomStatus.FULL) {
                throw new BusinessException(ErrorCode.FULL_AUCTION_ROOM);
            }
        }

        // 해당 시간 경매방이 5개이상
        if (rooms.size() >= 5) {
            throw new BusinessException(ErrorCode.AUCTION_ROOM_ASSIGN_UNAVAILABLE);
        }

        // 없으면 새 경매방 생성
        AuctionRoom newRoom = new AuctionRoom(liveTime, roomIndex);

        auctionRoomRepository.save(newRoom);
        return newRoom;

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processScheduledToLive(Long roomId) {
        AuctionRoom room = auctionRoomRepository.findById(roomId)
            .orElse(null);

        if (room == null) {
            log.warn("경매방이 없음: roomId={}", roomId);
            return;
        }

        //상태 재확인
        if (room.getAuctionStatus() != AuctionStatus.SCHEDULED) {
            log.warn("이미 처리된 경매방: roomId={}, status={}", roomId, room.getAuctionStatus());
            return;
        }

        //시간 재확인
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime entryAllowedTime = room.getLiveTime().minusMinutes(10);
        if (now.isBefore(entryAllowedTime)) {
            log.warn("아직 입장 가능 시간이 아님: roomId={}, liveTime={}", roomId, room.getLiveTime());
            return;
        }

        //상태 변경
        try {
            room.startLive();
            auctionRoomRepository.save(room);
            log.info("경매방 상태 변경 완료: roomId={}, liveTime={}", roomId, room.getLiveTime());

            List<Long> itemIds = room.getLiveItems().stream()
                .map(LiveItem::getId)
                .toList();

            eventPublisher.publishEvent(
                new AuctionRoomStartedEvent(
                    roomId,
                    room.getLiveTime(),
                    itemIds
                )
            );
        } catch (Exception e) {
            log.error("경매방 상태 변경 실패: roomId={}", roomId, e);
            throw e;
        }
    }

    public void deleteAuctionRoom(AuctionRoom auctionRoom) {
        auctionRoomRepository.delete(auctionRoom);
    }

    @Transactional(readOnly = true)
    public AuctionRoomListResponse getAuctionRooms(LocalDateTime targetTime, Long userId) {

        List<AuctionRoom> rooms = auctionRoomRepository.findRoomsWithItemsByLiveTime(targetTime);

        // ========== 찜 여부 조회 준비 ==========
        List<Long> liveItemIds = rooms.stream()
            .flatMap(room -> room.getLiveItems().stream())
            .map(LiveItem::getId)
            .toList();

        Set<Long> likedSet = Collections.emptySet();
        if (userId != null && !liveItemIds.isEmpty()) {
            likedSet = new HashSet<>(likeLiveRepository.findLikedLiveItemIds(userId, liveItemIds));
        }
        final Set<Long> finalLikedSet = likedSet;

        List<AuctionRoomDto> response = rooms.stream()
            .map(room -> {
                List<LiveItemDto> items = room.getLiveItems().stream()
                    .map(item -> {

                        String redisKey = "liveItem:" + item.getId();
                        String maxBidPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");

                        Long currentMaxBidPrice = (maxBidPriceStr != null)
                            ? Long.parseLong(maxBidPriceStr)
                            : item.getInitPrice();

                        boolean isLiked = finalLikedSet.contains(item.getId());

                        return new LiveItemDto(
                            item.getId(),
                            item.getName(),              // title로 내려줄 값
                            currentMaxBidPrice,         // amount로 내려줄 값(원하면 currentPrice 등으로 교체)
                            item.getThumbnail(),          // thumbnail 컬럼
                            isLiked
                        );
                    })
                    .toList();

                return new AuctionRoomDto(
                    room.getId(),
                    room.getRoomIndex(),
                    room.getAuctionStatus(),                // AuctionStatus enum
                    (long) items.size(),
                    items
                );
            })
            .toList();

        return new AuctionRoomListResponse(targetTime, response);

    }

    public AuctionScheduleResponse getAuctionSchedule(LocalDate fromDate, LocalDate toDate) {

        final int slotMinutes = 30;
        final int startHour = 9;
        final int endHour = 21;

        LocalDateTime fromAt = fromDate.atStartOfDay();
        LocalDateTime toAt = toDate.plusDays(1).atStartOfDay();

        List<RoomCountByStartAtRow> rows =
            auctionRoomRepository.countRoomsGroupedByStartAt(fromAt, toAt);

        Map<LocalDate, List<AuctionRoomSlotDto>> slotsByDate = new LinkedHashMap<>();

        for (RoomCountByStartAtRow row : rows) {
            LocalDateTime startAt = row.getStartAt();
            LocalDate date = startAt.toLocalDate();

            slotsByDate.computeIfAbsent(date, d -> new ArrayList<>())
                .add(new AuctionRoomSlotDto(startAt, row.getRoomCount().intValue()));
        }

        // days 리스트로 변환 (정렬 유지)
        List<AuctionDaysDto> days = slotsByDate.entrySet().stream()
            .map(e -> new AuctionDaysDto(e.getKey(), e.getValue()))
            .toList();

        return new AuctionScheduleResponse(
            fromDate,
            toDate,
            slotMinutes,
            startHour,
            endHour,
            days
        );

    }

    public AuctionRoomResponse getAuctionRoom(Long auctionRoomId) {

        AuctionRoom room = auctionRoomRepository.findRoomWithItems(auctionRoomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_ROOM_NOT_FOUND));

        List<LiveItem> items = liveItemRepository.findItemsWithImagesByRoomId(auctionRoomId);

        List<AuctionRoomItemDto> response  = items.stream()
            .map(item -> new AuctionRoomItemDto(
                item.getId(),
                item.getName(),
                item.getImages().stream()
                    .map(LiveItemImage::getImageUrl)
                    .toList(),
                item.getInitPrice(),
                item.getCurrentPrice(),
                item.getAuctionStatus()
            ))
            .toList();

        return new AuctionRoomResponse(response);

    }
}
