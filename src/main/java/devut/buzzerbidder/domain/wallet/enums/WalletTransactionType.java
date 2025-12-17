package devut.buzzerbidder.domain.wallet.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum WalletTransactionType {

    // 증가 계열
    CHARGE(true),              // 충전
    REFUND(true),              // 취소/실패 환불
    BID_REFUND(true),          // 입찰 실패 시 환불
    RECEIVE_FROM_USER(true),   // 다른 유저에게서 받음
    ADMIN_GRANT(true),         // 관리자 지급

    // 감소 계열
    PAY_TO_USER(false),         // 다른 유저에게 보냄
    BID_LOCK(false),            // 입찰 시 코인 차감
    WITHDRAW(false),            // 출금
    ADMIN_DEDUCT(false);        // 관리자 차감

    private final boolean isIncrease;

    public boolean isIncrease() {
        return isIncrease;
    }
}