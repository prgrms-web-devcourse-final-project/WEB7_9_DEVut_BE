package devut.buzzerbidder.domain.auctionroom.entity;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class AuctionRoom extends BaseEntity {

    private LocalDateTime liveTime;

    @Enumerated(EnumType.STRING)
    private RoomStatus roomStatus;

    @Enumerated(EnumType.STRING)
    private AuctionStatus auctionStatus;

    private long roomIndex;

    public enum RoomStatus {
        OPEN, FULL
    }

    public enum AuctionStatus {
        SCHEDULED,
        LIVE,
        ENDED
    }

    @OneToMany(mappedBy = "auctionRoom")
    private List<LiveItem> liveItems = new ArrayList<>();

    public AuctionRoom(LocalDateTime liveTime, Long roomIndex) {
        this.liveTime = liveTime;
        this.roomStatus = RoomStatus.OPEN;
        this.auctionStatus = AuctionStatus.SCHEDULED;
        this.roomIndex = roomIndex;
    }

    public boolean isFull() {
        return liveItems.size() >= 6;
    }

    public void addItem(LiveItem liveItem) {
        if (isFull()) {
            throw new BusinessException(ErrorCode.FULL_AUCTION_ROOM);
        }
        this.liveItems.add(liveItem);
        System.out.println("현재 아이템 개수: " + this.liveItems.size());

        liveItem.setAuctionRoom(this);
        if (isFull()) {
            this.roomStatus = RoomStatus.FULL;
        }
    }


    public void removeItem(LiveItem item) {
        // 1. 리스트에서 제거
        this.liveItems.remove(item);

        // 2. 상태 재계산
        recalcStatus();
    }

    private void recalcStatus() {
        if (this.liveItems.size() >= 6) {
            this.roomStatus = RoomStatus.FULL;
        } else {
            this.roomStatus = RoomStatus.OPEN;
        }
    }

    public void startLive() {
        if (this.auctionStatus != AuctionStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.AUCTION_STATUS_INVALID);
        }
        this.auctionStatus = AuctionStatus.LIVE;
    }

    public void endLive() {
        if (this.auctionStatus != AuctionStatus.LIVE) {
            throw new BusinessException(ErrorCode.AUCTION_STATUS_INVALID);
        }
        this.auctionStatus = AuctionStatus.ENDED;
    }
}
