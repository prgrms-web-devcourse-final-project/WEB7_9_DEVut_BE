package devut.buzzerbidder.domain.deal.entity;

import devut.buzzerbidder.domain.deal.enums.Carrier;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LiveDeal extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private LiveItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User buyer;

    @Column(nullable = false)
    private Long winningPrice;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DealStatus status;

    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    private Carrier carrier;

    public void updateDeliveryInfo(String carrierCode, String trackingNumber) {
        this.carrier = Carrier.fromCode(carrierCode);
        this.trackingNumber = trackingNumber;
    }

    public void updateStatus(DealStatus status) {
        this.status = status;
    }
}
