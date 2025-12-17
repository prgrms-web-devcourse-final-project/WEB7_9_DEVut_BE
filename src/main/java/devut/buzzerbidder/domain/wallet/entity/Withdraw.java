package devut.buzzerbidder.domain.wallet.entity;

import devut.buzzerbidder.domain.wallet.enums.WithdrawStatus;
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
public class Withdraw extends BaseEntity {

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
    private WithdrawStatus status;

    @Column(length = 255)
    private String msg;

    private LocalDateTime processedAt;

    public Withdraw(User user, Long amount, String bankName, String accountNumber, String accountHolder) {
        this.user = user;
        this.amount = amount;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.status = WithdrawStatus.REQUESTED;
    }

    public void approve() {
        this.status = WithdrawStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
        this.msg = "출금 승인 완료";
    }

    public void fail() {
        this.status = WithdrawStatus.FAILED;
        this.processedAt = LocalDateTime.now();
        this.msg = "출금 실패";
    }
}
