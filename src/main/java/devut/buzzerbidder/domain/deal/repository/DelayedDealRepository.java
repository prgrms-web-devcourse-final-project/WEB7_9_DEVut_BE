package devut.buzzerbidder.domain.deal.repository;

import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DelayedDealRepository extends JpaRepository<DelayedDeal, Long> {

    // DelayedItem으로 Deal 조회 (중복 생성 방지)
    Optional<DelayedDeal> findByItem(DelayedItem item);

    @Query("""
        SELECT dd FROM DelayedDeal dd
        JOIN FETCH dd.item di
        JOIN FETCH di.images
        JOIN FETCH dd.buyer
        WHERE dd.buyer = :buyer
        ORDER BY dd.createDate DESC
        """)
    List<DelayedDeal> findByBuyerWithItemAndImages(@Param("buyer") User buyer);

    @Query("""
        SELECT dd FROM DelayedDeal dd
        JOIN FETCH dd.item di
        JOIN FETCH di.images
        JOIN FETCH dd.buyer
        WHERE dd.id = :dealId
        """)
    java.util.Optional<DelayedDeal> findByIdWithItemAndImages(@Param("dealId") Long dealId);

    @Query("""
        SELECT DISTINCT dd FROM DelayedDeal dd
        JOIN FETCH dd.item di
        JOIN FETCH di.images
        JOIN FETCH dd.buyer
        WHERE dd.id IN :ids
        """)
    List<DelayedDeal> findByIdsWithItemAndImages(@Param("ids") List<Long> ids);

    // 관리자 조회용
    Page<DelayedDeal> findAll(Pageable pageable);
    Page<DelayedDeal> findByBuyerId(Long buyerId, Pageable pageable);
    Page<DelayedDeal> findByStatus(DealStatus status, Pageable pageable);
    Page<DelayedDeal> findByBuyerIdAndStatus(Long buyerId, DealStatus status, Pageable pageable);
}
