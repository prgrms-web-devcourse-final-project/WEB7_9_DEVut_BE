package devut.buzzerbidder.domain.likedelayed.repository;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.likedelayed.entity.LikeDelayed;
import devut.buzzerbidder.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikeDelayedRepository extends JpaRepository<LikeDelayed, Long> {

    Optional<LikeDelayed> findByUserAndDelayedItem(User user, DelayedItem delayedItem);

    long countByDelayedItemId(Long delayedItemId);

    @Query("SELECT l.delayedItem.id, COUNT(l) FROM LikeDelayed l WHERE l.delayedItem.id IN :ids GROUP BY l.delayedItem.id")
    List<Object[]> countByDelayedItemIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT ld.delayedItem.id FROM LikeDelayed ld WHERE ld.user.id = :userId")
    List<Long> findDelayedItemIdsByUserId(@Param("userId") Long userId);
}
