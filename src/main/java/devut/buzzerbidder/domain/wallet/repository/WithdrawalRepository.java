package devut.buzzerbidder.domain.wallet.repository;

import devut.buzzerbidder.domain.wallet.entity.Withdrawal;
import devut.buzzerbidder.domain.wallet.enums.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    Page<Withdrawal> findByStatus(WithdrawalStatus lastStatus, Pageable pageable);

    Page<Withdrawal> findByUserIdAndStatus(Long userId, WithdrawalStatus lastStatus, Pageable pageable);

    Page<Withdrawal> findByUserId(Long userId, Pageable pageable);
}
