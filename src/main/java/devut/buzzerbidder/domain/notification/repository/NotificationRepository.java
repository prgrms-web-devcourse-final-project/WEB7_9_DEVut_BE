package devut.buzzerbidder.domain.notification.repository;

import devut.buzzerbidder.domain.notification.entity.Notification;
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

}
