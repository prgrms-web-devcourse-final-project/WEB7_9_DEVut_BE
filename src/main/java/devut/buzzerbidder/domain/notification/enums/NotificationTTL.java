package devut.buzzerbidder.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 타입별 보관 기간(TTL) 전략
 */
@Getter
@RequiredArgsConstructor
public enum NotificationTTL {
    /**
     * 일반 알림
     * - 읽은 알림: 30일 (고객이 확인한 정보는 단기 보관)
     * - 읽지 않은 알림: 90일 (미확인 알림은 충분히 대기)
     */
    NORMAL(30, 90),

    /**
     * 거래/법적 증빙성 알림
     * - 읽음/안 읽음 무관하게 365일 보관
     * - 낙찰, 결제 완료, 거래 취소 등
     * - CS 대응, 분쟁 해결, 감사 목적
     */
    LEGAL(365, 365),

    /**
     * 시스템/정책 알림
     * - 보안, 약관 변경, 서비스 점검 등
     * - 읽음/안 읽음 무관하게 180일 보관
     */
    SYSTEM(180, 180);

    /**
     * 읽은 알림 보관 기간 (일)
     */
    private final int readRetentionDays;

    /**
     * 읽지 않은 알림 보관 기간 (일)
     */
    private final int unreadRetentionDays;
}