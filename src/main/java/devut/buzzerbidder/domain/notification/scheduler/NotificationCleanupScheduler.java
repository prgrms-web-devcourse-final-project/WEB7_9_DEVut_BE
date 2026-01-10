package devut.buzzerbidder.domain.notification.scheduler;

import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 자동 만료 스케줄러
 * - 매일 새벽 3시에 실행
 * - 타입별 TTL 정책에 따라 만료된 알림 삭제
 * - 일반 알림: 읽음 30일 / 안 읽음 90일
 * - 법적 증빙 알림: 365일
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;

    /**
     * 알림 자동 만료 처리
     * - Cron: 매일 새벽 3시 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredNotifications() {
        LocalDateTime now = LocalDateTime.now();

        // 모든 알림 타입에 대해 TTL 정책 적용
        for (NotificationType type : NotificationType.values()) {
            try {
                int deleted = cleanupByType(type, now);

                if (deleted > 0) {
                    log.info("알림 삭제 완료: type={}, count={}, readRetention={}일, unreadRetention={}일",
                        type,
                        deleted,
                        type.getReadRetentionDays(),
                        type.getUnreadRetentionDays()
                    );
                }
            } catch (Exception e) {
                log.error("알림 삭제 실패: type={}", type, e);
            }
        }
    }

    /**
     * 타입별 만료 알림 삭제
     */
    private int cleanupByType(NotificationType type, LocalDateTime now) {
        LocalDateTime readThreshold = now.minusDays(type.getReadRetentionDays());
        LocalDateTime unreadThreshold = now.minusDays(type.getUnreadRetentionDays());

        return notificationRepository.deleteExpiredNotificationsByType(
            type,
            readThreshold,
            unreadThreshold
        );
    }
}