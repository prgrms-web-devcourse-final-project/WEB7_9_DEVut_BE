package devut.buzzerbidder.domain.payment.entity;

public enum WithdrawStatus {
    REQUESTED,      // 출금 요청 완료
    APPROVED,       // 출금 승인
    FAILED          // 출금 실패
}
