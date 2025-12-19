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

    @Query("""
        SELECT ld.delayedItem FROM LikeDelayed ld
        LEFT JOIN FETCH ld.delayedItem.images
        WHERE ld.user.id = :userId
        ORDER BY ld.createDate DESC
        """)
    List<DelayedItem> findLikedDelayedItemsByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(ld) FROM LikeDelayed ld
        WHERE ld.user.id = :userId
        """)
    long countByUserId(@Param("userId") Long userId);
}
