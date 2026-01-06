package devut.buzzerbidder.domain.liveitem.repository;

import devut.buzzerbidder.domain.liveitem.dto.response.LiveItemResponse;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.Category;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
        li.currentPrice,
        null
    )
    FROM LiveItem li
    WHERE (:name IS NULL OR LOWER(li.name) LIKE %:name%)
      AND (:category IS NULL OR li.category = :category)
      AND (
                  :isSelling IS NULL OR :isSelling = false
                  OR li.auctionStatus IN (
                      devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.BEFORE_BIDDING,
                      devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.IN_PROGRESS
                  )
                )
""")
    Page<LiveItemResponse> searchLiveItems(
            @Param("name") String name,
            @Param("category") Category category,
            @Param("isSelling") Boolean isSelling,
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
        li.currentPrice,
        null
        
    )
    FROM LiveItem li
    LEFT JOIN LikeLive ll ON ll.liveItem = li
    WHERE li.auctionStatus IN (
            devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.BEFORE_BIDDING,
            devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.IN_PROGRESS
        )
    GROUP BY li.id, li.name, li.thumbnail, li.liveTime, li.auctionStatus, li.initPrice, li.currentPrice
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

    // 통합 검색용 : AuctionRoom JOIN
    @EntityGraph(attributePaths = {"auctionRoom"})
    @Query("SELECT DISTINCT li FROM LiveItem li WHERE li.id IN :ids")
    List<LiveItem> findLiveItemsWithAuctionRoom(@Param("ids") List<Long> ids);

    // 통합 검색용: 상태별 조회 (images + auctionRoom)
    @EntityGraph(attributePaths = {"images", "auctionRoom"})
    @Query("SELECT li FROM LiveItem li WHERE li.auctionStatus IN :statuses")
    List<LiveItem> findByAuctionStatusInWithImages(@Param("statuses") List<AuctionStatus> statuses);

    @Query("""
      SELECT new devut.buzzerbidder.domain.liveitem.dto.response.LiveItemResponse(
          li.id,
          li.name,
          li.thumbnail,
          li.liveTime,
          li.auctionStatus,
          li.initPrice,
          li.currentPrice,
          null
      )
      FROM LiveItem li
      WHERE (:name IS NULL OR LOWER(li.name) LIKE %:name%)
        AND (:category IS NULL OR li.category = :category)
        AND (:minPrice IS NULL OR li.initPrice >= :minPrice)
        AND (:maxPrice IS NULL OR li.initPrice <= :maxPrice)
        AND (
            :isSelling = false
            OR li.auctionStatus IN (
                devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.BEFORE_BIDDING,
                devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.IN_PROGRESS
            )
        )
  """)
    Page<LiveItemResponse> searchLiveItemsForAuction(
            @Param("name") String name,
            @Param("category") Category category,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("isSelling") Boolean isSelling,
            Pageable pageable
    );

    // 관리자 조회용
    Page<LiveItem> findByCategory(LiveItem.Category category, Pageable pageable);
    Page<LiveItem> findBySellerUserId(Long sellerUserId, Pageable pageable);
    Page<LiveItem> findBySellerUserIdAndCategory(Long sellerUserId, LiveItem.Category category, Pageable pageable);

    @Query("""
    SELECT li.id
    FROM LiveItem li
    WHERE (:name IS NULL OR LOWER(li.name) LIKE CONCAT('%', LOWER(:name), '%'))
      AND (:category IS NULL OR li.category = :category)
      AND (
            :isSelling IS NULL OR :isSelling = false
            OR li.auctionStatus IN (
                devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.BEFORE_BIDDING,
                devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.IN_PROGRESS
            )
          )
      AND (:minPrice IS NULL OR li.initPrice >= :minPrice)
      AND (:maxPrice IS NULL OR li.initPrice <= :maxPrice)
""")

    List<Long> findIdsByInitPriceRangeWithBaseFilters(
        @Param("name") String name,
        @Param("category") Category category,
        @Param("isSelling") Boolean isSelling,
        @Param("minPrice") Long minPrice,
        @Param("maxPrice") Long maxPrice
);

    @Query(
        value = """
        SELECT new devut.buzzerbidder.domain.liveitem.dto.response.LiveItemResponse(
            li.id,
            li.name,
            li.thumbnail,
            li.liveTime,
            li.auctionStatus,
            li.initPrice,
            li.currentPrice,
            null
        )
        FROM LiveItem li
        WHERE li.id IN (:ids)
          AND (:name IS NULL OR LOWER(li.name) LIKE CONCAT('%', LOWER(:name), '%'))
          AND (:category IS NULL OR li.category = :category)
          AND (
                :isSelling IS NULL OR :isSelling = false
                OR li.auctionStatus IN (
                    devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.BEFORE_BIDDING,
                    devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.IN_PROGRESS
                )
              )
        """,
        countQuery = """
        SELECT COUNT(li.id)
        FROM LiveItem li
        WHERE li.id IN (:ids)
          AND (:name IS NULL OR LOWER(li.name) LIKE CONCAT('%', LOWER(:name), '%'))
          AND (:category IS NULL OR li.category = :category)
          AND (
                :isSelling IS NULL OR :isSelling = false
                OR li.auctionStatus IN (
                    devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.BEFORE_BIDDING,
                    devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus.IN_PROGRESS
                )
              )
        """
)
    Page<LiveItemResponse> searchLiveItemsWithinIds(
        @Param("ids") Collection<Long> ids,
        @Param("name") String name,
        @Param("category") Category category,
        @Param("isSelling") Boolean isSelling,
        Pageable pageable
    );

    @Query("""
    select li.id
    from LiveItem li
    where li.auctionRoom.id = :auctionRoomId
      and li.id > :currentItemId
    order by li.id asc
""")
    List<Long> findNextItemIds(
            @Param("auctionRoomId") Long auctionRoomId,
            @Param("currentItemId") Long currentItemId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select li from LiveItem li where li.id = :id")
    Optional<LiveItem> findByIdWithLock(@Param("id") Long id);
}
