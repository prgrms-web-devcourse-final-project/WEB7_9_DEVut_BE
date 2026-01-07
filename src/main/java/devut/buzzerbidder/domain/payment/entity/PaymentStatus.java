package devut.buzzerbidder.domain.payment.entity;

public enum PaymentStatus {
    PENDING,        // 결제 진행중
    SUCCESS,        // 결제 성공
    FAILED,         // 결제 실패
    LOCKED,         // 결제 락(동시주문 방지)
    CANCEL_PENDING, // 결제 취소 대기중
    CANCELED,       // 결제 취소 성공
    CANCEL_FAILED   // 걀제 취소 재시도 3번도 실패
}
