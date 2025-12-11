package devut.buzzerbidder.global.notification.repository;

import devut.buzzerbidder.global.notification.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreateDateDesc(Long userId);

    List<Notification> findByUserIdAndCheckFalseOrderByCreateDateDesc(Long userId);

    Long countByUserIdAndCheckFalse(Long userId);

}
