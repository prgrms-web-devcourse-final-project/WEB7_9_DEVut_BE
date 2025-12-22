package devut.buzzerbidder.domain.likedelayed.entity;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
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
    name = "like_delayed",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "delayed_item_id"})
    },
    indexes = {
        @Index(name = "idx_like_delayed_item", columnList = "delayed_item_id")
    }
)
public class LikeDelayed extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delayed_item_id", nullable = false)
    private DelayedItem delayedItem;

    public LikeDelayed(User user, DelayedItem delayedItem) {
        this.user = user;
        this.delayedItem = delayedItem;
    }

}
