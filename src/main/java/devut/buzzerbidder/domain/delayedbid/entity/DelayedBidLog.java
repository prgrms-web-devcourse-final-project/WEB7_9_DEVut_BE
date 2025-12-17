package devut.buzzerbidder.domain.delayedbid.entity;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(indexes = {
    @Index(name = "idx_delayed_bid_item", columnList = "delayed_item_id"),
    @Index(name = "idx_delayed_bid_user", columnList = "bidder_user_id")
})
public class DelayedBidLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delayed_item_id", nullable = false)
    private DelayedItem delayedItem;

    @Column(name = "bidder_user_id", nullable = false)
    @NotNull
    private Long bidderUserId;

    @Column(name = "bid_amount", nullable = false)
    @NotNull
    @Positive
    private Long bidAmount;

    @Column(name = "bid_time", nullable = false)
    @NotNull
    private LocalDateTime bidTime;

    @Column(name = "is_highest")
    private Boolean isHighest;

    public DelayedBidLog(DelayedItem item, Long bidderUserId, Long bidAmount) {
        this.delayedItem = item;
        this.bidderUserId = bidderUserId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
        this.isHighest = true;
    }

    public void updateHighestStatus(Boolean isHighest) {
        this.isHighest = isHighest;
    }

}
