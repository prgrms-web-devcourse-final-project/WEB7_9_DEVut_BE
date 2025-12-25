package devut.buzzerbidder.domain.notification.repository;

import devut.buzzerbidder.domain.notification.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreateDateDesc(Long userId);

    List<Notification> findByUserIdAndIsCheckedFalseOrderByCreateDateDesc(Long userId);

    Long countByUserIdAndIsCheckedFalse(Long userId);

}
