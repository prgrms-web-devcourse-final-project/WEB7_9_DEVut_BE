package devut.buzzerbidder.domain.payment.entity;

import devut.buzzerbidder.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    private Long canceledAmount;

    @Column(length = 255)
    private String cancelReason;

    @Column(length = 100)
    private String failCode;

    @Column(length = 255)
    private String failReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private OffsetDateTime approvedAt;

    private LocalDateTime canceledAt;

    public static Payment create(
            User user,
            String orderId,
            String orderName,
            Long amount
    ) {
        Payment payment = new Payment();
        payment.user = user;
        payment.orderId = orderId;
        payment.orderName = orderName;
        payment.amount = amount;
        payment.status = PaymentStatus.PENDING;
        return payment;
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

    public void cancel(
            Long amount,
            String msg,
            LocalDateTime cancelledAt
    ) {
        this.canceledAmount += amount;
        this.cancelReason = msg;
        this.canceledAt = cancelledAt;
        this.status = PaymentStatus.CANCELED;
    }

    public boolean isPending() { return PaymentStatus.PENDING.equals(status); }

    public boolean isSuccess() { return PaymentStatus.SUCCESS.equals(status); }
}


