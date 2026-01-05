package devut.buzzerbidder.domain.deal.repository;

import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.user.entity.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveDealRepository extends JpaRepository<LiveDeal, Long> {

    @Query("""
        SELECT ld FROM LiveDeal ld
        JOIN FETCH ld.item li
        JOIN FETCH li.images
        JOIN FETCH ld.buyer
        WHERE ld.buyer = :buyer
        ORDER BY ld.createDate DESC
        """)
    List<LiveDeal> findByBuyerWithItemAndImages(@Param("buyer") User buyer);

    @Query("""
        SELECT ld FROM LiveDeal ld
        JOIN FETCH ld.item li
        JOIN FETCH li.images
        JOIN FETCH ld.buyer
        WHERE ld.id = :dealId
        """)
    java.util.Optional<LiveDeal> findByIdWithItemAndImages(@Param("dealId") Long dealId);

    @Query("""
        SELECT DISTINCT ld FROM LiveDeal ld
        JOIN FETCH ld.item li
        JOIN FETCH li.images
        JOIN FETCH ld.buyer
        WHERE ld.id IN :ids
        """)
    List<LiveDeal> findByIdsWithItemAndImages(@Param("ids") List<Long> ids);

    // 관리자 조회용
    Page<LiveDeal> findAll(Pageable pageable);
    Page<LiveDeal> findByBuyerId(Long buyerId, Pageable pageable);
    Page<LiveDeal> findByStatus(DealStatus status, Pageable pageable);
    Page<LiveDeal> findByBuyerIdAndStatus(Long buyerId, DealStatus status, Pageable pageable);
}
