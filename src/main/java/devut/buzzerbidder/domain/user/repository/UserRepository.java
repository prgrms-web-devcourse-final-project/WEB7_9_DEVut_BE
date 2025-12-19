package devut.buzzerbidder.domain.user.repository;

import devut.buzzerbidder.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    Optional<User> findByEmail(String email);

    @Query(value = """
        SELECT id, 'LIVE' as type, create_date as createdAt
        FROM live_item
        WHERE seller_user_id = :sellerUserId
        UNION ALL
        SELECT id, 'DELAYED' as type, create_date as createdAt
        FROM delayed_item
        WHERE seller_user_id = :sellerUserId
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findMyItemIdsAndTypes(
        @Param("sellerUserId") Long sellerUserId,
        @Param("limit") int limit,
        @Param("offset") long offset
    );

    @Query(value = """
        SELECT 
            (SELECT COUNT(*) FROM live_item WHERE seller_user_id = :sellerUserId) + 
            (SELECT COUNT(*) FROM delayed_item WHERE seller_user_id = :sellerUserId)
        """, nativeQuery = true)
    long countMyItems(@Param("sellerUserId") Long sellerUserId);

    @Query(value = """
        SELECT li.id, 'LIVE' as type, ll.create_date as createdAt
        FROM like_live ll
        INNER JOIN live_item li ON ll.live_item_id = li.id
        WHERE ll.user_id = :userId
        UNION ALL
        SELECT di.id, 'DELAYED' as type, ld.create_date as createdAt
        FROM like_delayed ld
        INNER JOIN delayed_item di ON ld.delayed_item_id = di.id
        WHERE ld.user_id = :userId
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findMyLikedItemIdsAndTypes(
        @Param("userId") Long userId,
        @Param("limit") int limit,
        @Param("offset") long offset
    );

    @Query(value = """
        SELECT 
            (SELECT COUNT(*) FROM like_live ll WHERE ll.user_id = :userId) + 
            (SELECT COUNT(*) FROM like_delayed ld WHERE ld.user_id = :userId)
        """, nativeQuery = true)
    long countMyLikedItems(@Param("userId") Long userId);
}

