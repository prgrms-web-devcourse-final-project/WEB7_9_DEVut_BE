package devut.buzzerbidder.domain.payment.entity;

public enum PaymentStatus {
    PENDING,    // 결제 진행중
    SUCCESS,    // 결제 성공
    FAILED,     // 결제 실패
    CANCELED,   // 결제 취소
    REFUNDED    // 결제 환불
}
