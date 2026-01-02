package devut.buzzerbidder.domain.user.entity;

import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAddress extends BaseEntity {

    @JoinColumn(nullable = false)
    @OneToOne
    private User user;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String addressDetail;

    @Column(nullable = false)
    private String postalCode;

    @Builder
    private DeliveryAddress(User user, String address, String addressDetail, String postalCode) {
        if (user == null) {
            throw new BusinessException(ErrorCode.DELIVERY_ADDRESS_USER_NOT_NULL);
        }
        if (address == null || address.isBlank()) {
            throw new BusinessException(ErrorCode.DELIVERY_ADDRESS_NOT_VALID);
        }
        if (addressDetail == null || addressDetail.isBlank()) {
            throw new BusinessException(ErrorCode.DELIVERY_ADDRESS_DETAIL_NOT_VALID);
        }
        if (postalCode == null || postalCode.isBlank()) {
            throw new BusinessException(ErrorCode.DELIVERY_POSTAL_CODE_NOT_VALID);
        }
        this.user = user;
        this.address = address;
        this.addressDetail = addressDetail;
        this.postalCode = postalCode;
    }

    public void update(String address, String addressDetail, String postalCode) {
        if (address != null && !address.isBlank()) {
            this.address = address;
        }
        if (addressDetail != null && !addressDetail.isBlank()) {
            this.addressDetail = addressDetail;
        }
        if (postalCode != null && !postalCode.isBlank()) {
            this.postalCode = postalCode;
        }
    }

}
