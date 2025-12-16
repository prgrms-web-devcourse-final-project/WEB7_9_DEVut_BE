package devut.buzzerbidder.domain.delayeditem.repository;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DelayedItemRepository extends JpaRepository<DelayedItem, Long> {

    @Query("""
        SELECT di From DelayedItem di
        LEFT JOIN di.images img
        WHERE (:name IS NULL OR LOWER(di.name) LIKE %:name%)
        AND (:category IS NULL OR di.category = :category)
        AND (:minPrice IS NULL OR di.currentPrice >= :minPrice)
        AND (:maxPrice IS NULL OR di.currentPrice <= :maxPrice)
        """)
    Page<DelayedItem> searchDelayedItems(
        @Param("name") String name,
        @Param("category") Category category,
        @Param("minPrice") Long minPrice,
        @Param("maxPrice") Long maxPrice,
        Pageable pageable
    );

    @Query("SELECT di FROM DelayedItem di " +
        "LEFT JOIN FETCH di.images " +
        "WHERE di.id = :id")
    Optional<DelayedItem> findDelayedItemWithImagesById(
        @Param("id") Long id
    );

    @Query("""
        SELECT di.id FROM DelayedItem di
        LEFT JOIN LikeDelayed ld ON ld.delayedItem = di
        GROUP BY di.id
        ORDER BY COUNT(ld.id) DESC
        """)
    List<Long> findHotDelayedItems(Pageable pageable);

    @Query("""
        SELECT DISTINCT di FROM DelayedItem di
        LEFT JOIN FETCH di.images
        WHERE di.id IN :ids
        """)
    List<DelayedItem> findDelayedItemsWithImages(@Param("ids") List<Long> ids);

}
