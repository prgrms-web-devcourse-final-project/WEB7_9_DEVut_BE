package devut.buzzerbidder.domain.liveitem.repository;

import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemResponse;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.Category;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
        false
    )
    FROM LiveItem li
    WHERE (:name IS NULL OR LOWER(li.name) LIKE %:name%)
      AND (:category IS NULL OR li.category = :category)
""")
    Page<LiveItemResponse> searchLiveItems(
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

    @EntityGraph(attributePaths = {"auctionRoom"})
    @Query("SELECT DISTINCT li FROM LiveItem li WHERE li.id IN :ids")
    List<LiveItem> findLiveItemsWithAuctionRoom(@Param("ids") List<Long> ids);

    // 특정 상태의 라이브 경매 전체 조회 (이미지 + 경매방 포함)
    @EntityGraph(attributePaths = {"images", "auctionRoom"})
    @Query("SELECT li FROM LiveItem li WHERE li.auctionStatus IN :statuses")
    List<LiveItem> findByAuctionStatusInWithImages(@Param("statuses") List<AuctionStatus> statuses);
}
