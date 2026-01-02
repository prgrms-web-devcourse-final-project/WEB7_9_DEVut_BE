package devut.buzzerbidder.domain.likelive.repository;

import devut.buzzerbidder.domain.likelive.entity.LikeLive;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikeLiveRepository extends JpaRepository<LikeLive, Long> {

    Optional<LikeLive> findByUserAndLiveItem(User user, LiveItem liveItem);

    long countByLiveItemId(Long liveItemId);

    @Query("SELECT l.liveItem.id, COUNT(l) FROM LikeLive l WHERE l.liveItem.id IN :ids GROUP BY l.liveItem.id")
    List<Object[]> countByLiveItemIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT ll.liveItem.id FROM LikeLive ll WHERE ll.user.id = :userId")
    List<Long> findLiveItemIdsByUserId(@Param("userId") Long userId);
}