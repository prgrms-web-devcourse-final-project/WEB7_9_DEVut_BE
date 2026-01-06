package devut.buzzerbidder.domain.payment.entity;

public enum PaymentStatus {
    PENDING,        // 결제 진행중
    SUCCESS,        // 결제 성공
    FAILED,         // 결제 실패
    LOCKED,         // 결제 락(동시주문 방지)
    CANCELED,       // 결제 취소
    CANCEL_FAILED   // 결제 취소 요청 실패
}
