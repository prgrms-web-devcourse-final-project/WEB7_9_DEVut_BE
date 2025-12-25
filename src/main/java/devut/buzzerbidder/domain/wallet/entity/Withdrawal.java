package devut.buzzerbidder.domain.wallet.entity;

import devut.buzzerbidder.domain.wallet.enums.WithdrawalStatus;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Withdrawal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String accountHolder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status;

    @Column(length = 255)
    private String msg;

    @Column(length = 255)
    private String rejectReason;

    private LocalDateTime processedAt;

    public Withdrawal(User user, Long amount, String bankName, String accountNumber, String accountHolder) {
        this.user = user;
        this.amount = amount;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.status = WithdrawalStatus.REQUESTED;
    }

    public void approve() {
        this.status = WithdrawalStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
        this.msg = "출금 승인 완료";
    }

    public void reject(String rejectReason) {
        this.status = WithdrawalStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
        this.msg = "출금 승인 실패";
        this.rejectReason = rejectReason;
    }
}
