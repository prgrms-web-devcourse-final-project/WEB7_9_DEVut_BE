package devut.buzzerbidder.domain.delayeditem.entity;

import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class DelayedItemImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delayed_item_id")
    private DelayedItem delayedItem;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    public DelayedItemImage(String imageUrl, DelayedItem delayedItem) {
        this.imageUrl = imageUrl;
        this.delayedItem = delayedItem;
    }
}
