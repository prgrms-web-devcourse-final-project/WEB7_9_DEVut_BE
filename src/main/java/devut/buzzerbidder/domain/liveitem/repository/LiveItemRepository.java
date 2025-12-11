package devut.buzzerbidder.domain.liveitem.repository;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveItemRepository extends JpaRepository<LiveItem, Long> {

    @Query("""
        SELECT li FROM LiveItem li
        LEFT JOIN li.images img
        WHERE (:name IS NULL OR LOWER(li.name) LIKE %:name%)
        AND (:category IS NULL OR li.category = :category)
        """)
    Page<LiveItem> searchLiveItems(
        @Param("name") String name,
        @Param("category") Category category,
        Pageable pageable
    );

    @Query("SELECT li FROM LiveItem li " +
        "LEFT JOIN FETCH li.images " + // LiveItemImage 목록 Fetch Join
        "WHERE li.id = :id")
    Optional<LiveItem> findLiveItemWithImagesById(
        @Param("id") Long id
    );

    @Query("""
        SELECT li.id FROM LiveItem li
        LEFT JOIN LikeLive ll ON ll.liveItemId = li.id
        GROUP BY li.id
        ORDER BY COUNT(ll.id) DESC
        """)
    List<Long> findHotLiveItems(Pageable pageable);

    @Query("""
        SELECT DISTINCT li FROM LiveItem li
        LEFT JOIN FETCH li.images
        WHERE li.id IN :ids
        """)
    List<LiveItem> findLiveItemsWithImages(@Param("ids") List<Long> ids);

}