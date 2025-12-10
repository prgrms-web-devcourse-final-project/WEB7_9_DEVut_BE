package devut.buzzerbidder.global.notification.repository;

import devut.buzzerbidder.global.notification.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByMemberIdOrderByCreateDateDesc(Long memberId);

    List<Notification> findByMemberIdAndCheckFalseOrderByCreateDateDesc(Long memberId);

    Long countByMemberIdAndCheckFalse(Long memberId);

}
