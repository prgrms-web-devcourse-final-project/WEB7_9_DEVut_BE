package devut.buzzerbidder.domain.liveBid.entity;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LiveBidLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id",  nullable = false)
    private LiveItem liveItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private AuctionRoom auctionRoom;

    private int bidPrice;

    @Builder
    public LiveBidLog(User bidder, LiveItem liveItem, User seller, AuctionRoom auctionRoom, int bidPrice) {
        this.bidder = bidder;
        this.liveItem = liveItem;
        this.seller = seller;
        this.auctionRoom = auctionRoom;
        this.bidPrice = bidPrice;
    }
}
