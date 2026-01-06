package devut.buzzerbidder.domain.payment.repository;

import devut.buzzerbidder.domain.payment.entity.Payment;
import devut.buzzerbidder.domain.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);

    Page<Payment> findByUserIdAndApprovedAtBetween(Long id, OffsetDateTime startTime, OffsetDateTime endTime, Pageable pageable);

    Page<Payment> findByUserIdAndApprovedAtBetweenAndStatus(Long id, OffsetDateTime startTime, OffsetDateTime endTime, PaymentStatus paymentStatus, Pageable pageable);

    // 결제 중 동시성 제어를 위한 비관적 쓰기락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderId = :orderId")
    Optional<Payment> findByOrderIdForLock(@Param("orderId") String orderId);

    // 취소 재시도가 필요한 결제를 ID 오름차순으로 반환
    @Query("""
        select p from Payment p
        where p.status = devut.buzzerbidder.domain.payment.entity.PaymentStatus.CANCEL_PENDING
          and (p.nextCancelRetryAt is null or p.nextCancelRetryAt <= :now)
        order by p.id asc
    """)
    List<Payment> findDueCancelPending(@Param("now") OffsetDateTime now, Pageable pageable);
}
