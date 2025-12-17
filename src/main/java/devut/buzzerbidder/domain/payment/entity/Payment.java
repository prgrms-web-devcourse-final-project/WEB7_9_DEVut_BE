package devut.buzzerbidder.domain.payment.entity;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String orderId;

    @Column(length = 300)
    private String paymentKey;

    @Column(nullable = false, length = 100)
    private String orderName;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 100)
    private String failCode;

    @Column(length = 255)
    private String failReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private OffsetDateTime approvedAt;

    public Payment(User user, String orderId, String orderName, Long amount) {
        this.user = user;
        this.orderId = orderId;
        this.orderName = orderName;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public void confirm(
            String paymentKey,
            PaymentMethod method,
            OffsetDateTime approvedAt
    ) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.approvedAt = approvedAt;
        this.status = PaymentStatus.SUCCESS;
    }

    public void fail(
            String code,
            String msg
    ) {
        this.failCode = code;
        this.failReason = msg;
        this.status = PaymentStatus.FAILED;
    }

    public boolean isPending() { return PaymentStatus.PENDING.equals(status); }

    public boolean isSuccess() { return PaymentStatus.SUCCESS.equals(status); }
}


