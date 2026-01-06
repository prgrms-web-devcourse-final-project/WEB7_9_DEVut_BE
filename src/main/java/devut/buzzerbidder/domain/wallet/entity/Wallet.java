package devut.buzzerbidder.domain.wallet.entity;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private User user;

    @Setter
    @Column(nullable = false)
    private Long bizz;

    @Builder
    private Wallet(User user) {
        if (user == null) throw new BusinessException(ErrorCode.WALLET_USER_NOT_NULL);
        this.user = user;
        this.bizz = 0L;
    }

    public void increaseBizz(Long amount) {
        validateAmount(amount);
        this.bizz += amount;
    }

    public void decreaseBizz(Long amount) {
        validateAmount(amount);
        if (this.bizz < amount) {
            throw new BusinessException(ErrorCode.BIZZ_INSUFFICIENT_BALANCE);
        }
        this.bizz -= amount;
    }

    /* ==================== 헬퍼 메서드 ==================== */

    private void validateAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_WALLET_AMOUNT);
        }
    }

}
