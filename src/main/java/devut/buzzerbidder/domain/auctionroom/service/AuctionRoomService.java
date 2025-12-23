package devut.buzzerbidder.domain.auctionroom.service;

import static reactor.netty.http.HttpConnectionLiveness.log;

import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionRoomDto;
import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionRoomListResponse;
import devut.buzzerbidder.domain.auctionroom.dto.response.LiveItemDto;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.AuctionStatus;
import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom.RoomStatus;
import devut.buzzerbidder.domain.auctionroom.repository.AuctionRoomRepository;
import devut.buzzerbidder.domain.liveBid.service.LiveBidRedisService;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionRoomService {

    private final AuctionRoomRepository auctionRoomRepository;
    private final LiveBidRedisService liveBidRedisService;

    public AuctionRoom assignRoom(LocalDateTime liveTime, long roomIndex) {

        List<AuctionRoom> rooms = auctionRoomRepository.findAllByLiveTime(liveTime);

        AuctionRoom targetRoom = null;
        boolean hasOpenRoom = false;

        // 순회하면서 OPEN 있는지 검사 + roomIndex로 방찾기
        for (AuctionRoom room : rooms) {

            if (room.getRoomStatus() == RoomStatus.OPEN) {
                hasOpenRoom = true;
            }

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

        // 해당 시간 경매방이 5개이상 + OPEN 상태인 방이 없음
        if (rooms.size() >= 5 && !hasOpenRoom) {
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
        } catch (Exception e) {
            log.error("경매방 상태 변경 실패: roomId={}", roomId, e);
            throw e;
        }
    }

    public void deleteAuctionRoom(AuctionRoom auctionRoom) {
        auctionRoomRepository.delete(auctionRoom);
    }

    @Transactional(readOnly = true)
    public AuctionRoomListResponse getAuctionRooms(LocalDateTime targetTime) {

        List<AuctionRoom> rooms = auctionRoomRepository.findRoomsWithItemsByLiveTime(targetTime);

        List<AuctionRoomDto> response = rooms.stream()
            .map(room -> {
                List<LiveItemDto> items = room.getLiveItems().stream()
                    .map(item -> {

                        String redisKey = "liveItem:" + item.getId();
                        String maxBidPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");

                        Long currentMaxBidPrice = (maxBidPriceStr != null)
                            ? Long.parseLong(maxBidPriceStr)
                            : item.getInitPrice();

                        return new LiveItemDto(
                            item.getId(),
                            item.getName(),              // title로 내려줄 값
                            currentMaxBidPrice,         // amount로 내려줄 값(원하면 currentPrice 등으로 교체)
                            item.getThumbnail()          // thumbnail 컬럼
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
}
