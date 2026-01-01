package devut.buzzerbidder.domain.liveitem.repository;

import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemResponse;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.Category;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveItemRepository extends JpaRepository<LiveItem, Long> {


    @Query("""
    SELECT new devut.buzzerbidder.domain.liveitem.dto.response.LiveItemResponse(
        li.id,
        li.name,
        li.thumbnail,
        li.liveTime,
        li.auctionStatus,
        li.initPrice,
        null
    )
    FROM LiveItem li
    WHERE (:name IS NULL OR LOWER(li.name) LIKE %:name%)
      AND (:category IS NULL OR li.category = :category)
      AND (
                  :isSaling IS NULL OR :isSaling = false
                  OR li.auctionStatus IN (
                      devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.BEFORE_BIDDING,
                      devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.IN_PROGRESS
                  )
                )
""")
    Page<LiveItemResponse> searchLiveItems(
        @Param("name") String name,
        @Param("category") Category category,
        @Param("isSaling") Boolean isSaling,
        Pageable pageable
    );

    @Query("SELECT li FROM LiveItem li " +
        "LEFT JOIN FETCH li.images " + // LiveItemImage 목록 Fetch Join
        "WHERE li.id = :id")
    Optional<LiveItem> findLiveItemWithImagesById(
        @Param("id") Long id
    );

    @Query("""
    SELECT new devut.buzzerbidder.domain.liveitem.dto.response.LiveItemResponse(
        li.id,
        li.name,
        li.thumbnail,
        li.liveTime,
        li.auctionStatus,
        li.initPrice,
        false
    )
    FROM LiveItem li
    LEFT JOIN LikeLive ll ON ll.liveItem = li
    WHERE li.auctionStatus IN (
            devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.BEFORE_BIDDING,
            devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.IN_PROGRESS
        )
    GROUP BY li.id, li.name, li.thumbnail, li.liveTime, li.auctionStatus, li.initPrice
    ORDER BY COUNT(ll.id) DESC, li.id DESC
""")
    List<LiveItemResponse> findHotLiveItems(Pageable pageable);

    @Query("""
        SELECT DISTINCT li FROM LiveItem li
        LEFT JOIN FETCH li.images
        WHERE li.id IN :ids
        """)
    List<LiveItem> findLiveItemsWithImages(@Param("ids") List<Long> ids);

    @Query("SELECT li.id " +
            " FROM LiveItem li " +
            "WHERE li.auctionStatus = :status " +
            "  AND li.liveTime <= :time")
    List<Long> findIdsToStart(
            @Param("time") LocalDateTime time,
            @Param("status") LiveItem.AuctionStatus status
    );

    @Query("SELECT li FROM LiveItem li " +
            "WHERE li.auctionStatus = :status " +
            "AND li.liveTime <= :thresholdTime")
    List<LiveItem> findItemsToEnd(
            @Param("status") LiveItem.AuctionStatus status,
            @Param("thresholdTime") LocalDateTime thresholdTime
    );

    @Query("""
        select distinct li
        from LiveItem li
        left join fetch li.images img
        where li.auctionRoom.id = :auctionRoomId
        order by li.id asc
    """)
    List<LiveItem> findItemsWithImagesByRoomId(@Param("auctionRoomId") Long auctionRoomId);

}
