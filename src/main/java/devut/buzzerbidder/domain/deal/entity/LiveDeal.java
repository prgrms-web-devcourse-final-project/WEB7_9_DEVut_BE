package devut.buzzerbidder.domain.deal.entity;

import devut.buzzerbidder.domain.deal.enums.Carrier;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
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

    @JoinColumn(nullable = false, unique = true)
    private Long item; // TODO: Item Entity와 연관관계 설정 필요

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
}
