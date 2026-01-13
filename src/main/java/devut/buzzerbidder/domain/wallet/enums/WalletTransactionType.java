package devut.buzzerbidder.domain.wallet.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum WalletTransactionType {

    // 증가 계열
    CHARGE(true),               // 충전
    RECEIVE_FROM_USER(true),    // 다른 유저에게서 받음,
    BID_REFUND(true),           // 입찰 실패 시 환불
    RECIEVE_DEPOSIT(true),      // 보증금 수령
    REFUND(true),               // 취소/실패 환불
    RECEIVE_SETTLEMENT(true),   // 경매 정산 받기
    ADMIN_GRANT(true),          // 관리자 지급

    // 감소 계열
    PAY_TO_USER(false),         // 다른 유저에게 보냄
    BID(false),                 // 입찰 시 코인 차감
    WITHDRAW(false),            // 출금
    PAY_SETTLEMENT(false),      // 경매 정산 하기
    ADMIN_DEDUCT(false),

    // 안 쓰는 값(DB에는 남아있기 때문에 지우지 않음)
    DEAL_SETTLEMENT(true);

    private final boolean isIncrease;
}