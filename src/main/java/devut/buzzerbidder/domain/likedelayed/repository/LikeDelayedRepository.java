package devut.buzzerbidder.domain.likedelayed.repository;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.likedelayed.entity.LikeDelayed;
import devut.buzzerbidder.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeDelayedRepository extends JpaRepository<LikeDelayed, Long> {

    Optional<LikeDelayed> findByUserAndDelayedItem(User user, DelayedItem delayedItem);

    long countByDelayedItemId(Long delayedItemId);
}
