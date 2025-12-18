package devut.buzzerbidder.domain.payment.repository;

import devut.buzzerbidder.domain.payment.entity.Payment;
import devut.buzzerbidder.domain.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);

    Page<Payment> findByUserIdAndApprovedAtBetween(Long id, OffsetDateTime startTime, OffsetDateTime endTime, Pageable pageable);

    Page<Payment> findByUserIdAndApprovedAtBetweenAndStatus(Long id, OffsetDateTime startTime, OffsetDateTime endTime, PaymentStatus paymentStatus, Pageable pageable);

    Optional<Payment> findByUserIdAndOrderId(Long userId, String orderId);
}
