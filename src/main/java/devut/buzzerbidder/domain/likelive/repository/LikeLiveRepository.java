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

    @Query("SELECT l.user.id FROM LikeLive l WHERE l.liveItem.id = :liveItemId")
    List<Long> findUserIdsByLiveItemId(@Param("liveItemId") Long liveItemId);

    @Query("""
        select ll.liveItem.id
        from LikeLive ll
        where ll.user.id = :userId
          and ll.liveItem.id in (:liveItemIds)
    """)
    List<Long> findLikedLiveItemIds(
        @Param("userId") Long userId,
        @Param("liveItemIds") List<Long> liveItemIds);

    @Query("""
    select (count(ll) > 0)
    from LikeLive ll
    where ll.user.id = :userId
      and ll.liveItem.id = :liveItemId
""")
    boolean existsByUserIdAndLiveItemId(
        @Param("userId") Long userId,
        @Param("liveItemId") Long liveItemId
    );

    @Query("SELECT ll.liveItem.id FROM LikeLive ll WHERE ll.user.id = :userId")
    List<Long> findLiveItemIdsByUserId(@Param("userId") Long userId);
}