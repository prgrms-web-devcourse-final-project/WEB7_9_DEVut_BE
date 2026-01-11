package devut.buzzerbidder.domain.notification.repository;

import devut.buzzerbidder.domain.notification.entity.Notification;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreateDateDesc(Long userId);

    List<Notification> findByUserIdAndIsCheckedFalseOrderByCreateDateDesc(Long userId);

    Long countByUserIdAndIsCheckedFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isChecked = true WHERE n.userId = :userId AND n.isChecked = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) > 0 FROM Notification n " +
           "WHERE n.userId = :userId " +
           "AND n.type = :type " +
           "AND n.resourceId = :resourceId " +
           "AND n.metadata LIKE CONCAT('%', :keyword, '%')")
    boolean existsPaymentReminder(
        @Param("userId") Long userId,
        @Param("type") NotificationType type,
        @Param("resourceId") Long resourceId,
        @Param("keyword") String keyword
    );

    /**
     * TTL 기반 알림 삭제 (타입별 보관 기간 적용)
     * 읽은 알림: type별 readRetentionDays 기준
     * 안 읽은 알림: type별 unreadRetentionDays 기준
     */
    @Modifying
    @Query("DELETE FROM Notification n " +
           "WHERE n.type = :type " +
           "AND ((n.isChecked = true AND n.createDate < :readThreshold) " +
           "OR (n.isChecked = false AND n.createDate < :unreadThreshold))")
    int deleteExpiredNotificationsByType(
        @Param("type") NotificationType type,
        @Param("readThreshold") LocalDateTime readThreshold,
        @Param("unreadThreshold") LocalDateTime unreadThreshold
    );

}
