package devut.buzzerbidder.domain.wallet.repository;

import devut.buzzerbidder.domain.wallet.entity.Withdrawal;
import devut.buzzerbidder.domain.wallet.enums.WithdrawalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    Page<Withdrawal> findByStatus(WithdrawalStatus lastStatus, Pageable pageable);

    Page<Withdrawal> findByUserIdAndStatus(Long userId, WithdrawalStatus lastStatus, Pageable pageable);

    // 출금 거절 시, 동시성 처리 위해 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Withdrawal w where w.id = :withdrawalId")
    Optional<Withdrawal> findByIdForUpdate(@Param("withdrawalId")Long withdrawalId);

    Page<Withdrawal> findByUserId(Long userId, Pageable pageable);
}
