package devut.buzzerbidder.domain.delayedbid.repository;

import devut.buzzerbidder.domain.delayedbid.entity.DelayedBidLog;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DelayedBidRepository extends JpaRepository<DelayedBidLog, Long> {

    boolean existsByDelayedItem(DelayedItem delayedItem);

    @Query("""
        SELECT db FROM DelayedBidLog db
        WHERE db.delayedItem.id = :delayedItemId
        ORDER BY db.bidAmount DESC
        LIMIT 1
        """)
    Optional<DelayedBidLog> findTopByDelayedItemIdOrderByBidAmountDesc(
        @Param("delayedItemId") Long delayedItemId
    );

    Page<DelayedBidLog> findByDelayedItemOrderByBidTimeDesc(DelayedItem delayedItem, Pageable pageable);

    Page<DelayedBidLog> findByBidderUserIdOrderByBidTimeDesc(Long bidderUserId, Pageable pageable);
}
