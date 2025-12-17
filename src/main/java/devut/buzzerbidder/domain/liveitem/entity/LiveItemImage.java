package devut.buzzerbidder.domain.liveitem.entity;

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
public class LiveItemImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_item_id")
    private LiveItem liveItem;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    public LiveItemImage(String imageUrl, LiveItem liveItem) {
        this.imageUrl = imageUrl;
        this.liveItem = liveItem;
        liveItem.getImages().add(this);
    }


}
