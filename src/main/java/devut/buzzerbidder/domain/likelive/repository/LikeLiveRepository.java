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

    @Query("""
        SELECT ll.liveItem FROM LikeLive ll
        LEFT JOIN FETCH ll.liveItem.images
        WHERE ll.user.id = :userId
        ORDER BY ll.createDate DESC
        """)
    List<LiveItem> findLikedLiveItemsByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(ll) FROM LikeLive ll
        WHERE ll.user.id = :userId
        """)
    long countByUserId(@Param("userId") Long userId);
}