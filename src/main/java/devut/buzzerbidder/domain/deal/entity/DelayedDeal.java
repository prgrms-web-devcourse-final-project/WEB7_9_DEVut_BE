package devut.buzzerbidder.domain.deal.entity;

import devut.buzzerbidder.domain.deal.enums.Carrier;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DelayedDeal extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private DelayedItem item;

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

    @Column(length = 500)
    private String deliveryAddress;

    @Column(length = 500)
    private String deliveryAddressDetail;

    @Column(length = 10)
    private String deliveryPostalCode;

    public void updateDeliveryInfo(String carrierCode, String trackingNumber) {
        this.carrier = Carrier.fromCode(carrierCode);
        this.trackingNumber = trackingNumber;
    }

    public void updateDeliveryAddress(String address, String addressDetail, String postalCode) {
        if (address != null && !address.isBlank()) {
            this.deliveryAddress = address;
        }
        if (addressDetail != null && !addressDetail.isBlank()) {
            this.deliveryAddressDetail = addressDetail;
        }
        if (postalCode != null && !postalCode.isBlank()) {
            this.deliveryPostalCode = postalCode;
        }
    }

    public void updateStatus(DealStatus status) {
        this.status = status;
    }
}
