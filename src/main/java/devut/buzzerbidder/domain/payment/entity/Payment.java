package devut.buzzerbidder.domain.payment.entity;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    @Column(nullable = false)
    private int cancelRetryCount;

    private OffsetDateTime approvedAt;
    private OffsetDateTime nextCancelRetryAt;
    private OffsetDateTime lastCancelTriedAt;

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

    // 취소 요청 성공 시, 취소 완료 상태 변경
    public void canceled(
            String code,
            String msg
    ) {
        this.failCode = code;
        this.failReason = msg;
        this.status = PaymentStatus.CANCELED;
    }

    // 결제 취소 요청 싪패 시, 진행중으로 상태 변경
    public void cancelPending(
            String code,
            String msg,
            OffsetDateTime nextRetryAt
    ) {
        this.failCode = code;
        this.failReason = msg;
        this.status = PaymentStatus.CANCEL_PENDING;
        this.nextCancelRetryAt = nextRetryAt;
    }

    // 결제 취소 요청 싪패 시, 진행중으로 상태 변경
    public void failAfterThreeRetry(
            String code,
            String msg
    ) {
        this.failCode = code;
        this.failReason = msg;
        this.status = PaymentStatus.CANCEL_FAILED;
    }

    public void increaseCancelRetryCount(
            OffsetDateTime nextRetryAt
    ) {
        this.cancelRetryCount += 1;
        this.nextCancelRetryAt = nextRetryAt;
    }

    public void markLocked() {
        this.status = PaymentStatus.LOCKED;
    }

    public boolean isPending() { return PaymentStatus.PENDING.equals(status); }

    public boolean isLocked() { return PaymentStatus.LOCKED.equals(status); }
}


