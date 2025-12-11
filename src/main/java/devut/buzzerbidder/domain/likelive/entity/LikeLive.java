package devut.buzzerbidder.domain.likelive.entity;

import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Table(
    name = "like_live",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"member_id", "live_item_id"}
        )
    },
    indexes = {
        @Index(name = "idx_like_live_item", columnList = "live_item_id")
    }
)
public class LikeLive extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "live_item_id", nullable = false)
    private Long liveItemId;

    public LikeLive(Long memberId, Long liveItemId) {
        this.memberId = memberId;
        this.liveItemId = liveItemId;
    }

}
