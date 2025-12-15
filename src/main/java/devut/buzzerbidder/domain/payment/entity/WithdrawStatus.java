package devut.buzzerbidder.domain.payment.entity;

public enum WithdrawStatus {
    PENDING,    // 출금 진행 중
    REJECTED,   // 출금 거부
    APPROVED,   // 출금 승인
    FAILED      // 출금 실패
}
