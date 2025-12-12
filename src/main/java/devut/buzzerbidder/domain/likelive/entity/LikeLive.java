package devut.buzzerbidder.domain.likelive.entity;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Table(
    name = "like_live",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"user_id", "live_item_id"}
        )
    },
    indexes = {
        @Index(name = "idx_like_live_item", columnList = "live_item_id")
    }
)
public class LikeLive extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_item_id", nullable = false)
    private LiveItem liveItem;

    public LikeLive(User user, LiveItem liveItem) {
        this.user = user;
        this.liveItem = liveItem;
    }

}
