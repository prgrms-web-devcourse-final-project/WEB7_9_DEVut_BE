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
        AND (:type IS NULL OR :type = 'LIVE')
        UNION ALL
        SELECT id, 'DELAYED' as type, create_date as createdAt
        FROM delayed_item
        WHERE seller_user_id = :sellerUserId
        AND (:type IS NULL OR :type = 'DELAYED')
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findMyItemIdsAndTypes(
        @Param("sellerUserId") Long sellerUserId,
        @Param("type") String type,
        @Param("limit") int limit,
        @Param("offset") long offset
    );

    @Query(value = """
        SELECT 
            (SELECT COUNT(*) FROM live_item WHERE seller_user_id = :sellerUserId 
             AND (:type IS NULL OR :type = 'LIVE')) + 
            (SELECT COUNT(*) FROM delayed_item WHERE seller_user_id = :sellerUserId 
             AND (:type IS NULL OR :type = 'DELAYED'))
        """, nativeQuery = true)
    long countMyItems(@Param("sellerUserId") Long sellerUserId, @Param("type") String type);

    @Query(value = """
        SELECT li.id, 'LIVE' as type, ll.create_date as createdAt
        FROM like_live ll
        INNER JOIN live_item li ON ll.live_item_id = li.id
        WHERE ll.user_id = :userId
        AND (:type IS NULL OR :type = 'LIVE')
        UNION ALL
        SELECT di.id, 'DELAYED' as type, ld.create_date as createdAt
        FROM like_delayed ld
        INNER JOIN delayed_item di ON ld.delayed_item_id = di.id
        WHERE ld.user_id = :userId
        AND (:type IS NULL OR :type = 'DELAYED')
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findMyLikedItemIdsAndTypes(
        @Param("userId") Long userId,
        @Param("type") String type,
        @Param("limit") int limit,
        @Param("offset") long offset
    );

    @Query(value = """
        SELECT 
            (SELECT COUNT(*) FROM like_live ll WHERE ll.user_id = :userId 
             AND (:type IS NULL OR :type = 'LIVE')) + 
            (SELECT COUNT(*) FROM like_delayed ld WHERE ld.user_id = :userId 
             AND (:type IS NULL OR :type = 'DELAYED'))
        """, nativeQuery = true)
    long countMyLikedItems(@Param("userId") Long userId, @Param("type") String type);

    @Query(value = """
        SELECT ld.id, 'LIVE' as type, ld.create_date as createdAt
        FROM live_deal ld
        WHERE ld.buyer_id = :buyerId
        AND (:type IS NULL OR :type = 'LIVE')
        UNION ALL
        SELECT dd.id, 'DELAYED' as type, dd.create_date as createdAt
        FROM delayed_deal dd
        WHERE dd.buyer_id = :buyerId
        AND (:type IS NULL OR :type = 'DELAYED')
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findMyDealIdsAndTypes(
        @Param("buyerId") Long buyerId,
        @Param("type") String type,
        @Param("limit") int limit,
        @Param("offset") long offset
    );

    @Query(value = """
        SELECT 
            (SELECT COUNT(*) FROM live_deal WHERE buyer_id = :buyerId 
             AND (:type IS NULL OR :type = 'LIVE')) + 
            (SELECT COUNT(*) FROM delayed_deal WHERE buyer_id = :buyerId 
             AND (:type IS NULL OR :type = 'DELAYED'))
        """, nativeQuery = true)
    long countMyDeals(@Param("buyerId") Long buyerId, @Param("type") String type);
}

