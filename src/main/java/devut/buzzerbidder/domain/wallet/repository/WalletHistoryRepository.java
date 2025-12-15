package devut.buzzerbidder.domain.wallet.repository;

import devut.buzzerbidder.domain.wallet.entity.WalletHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletHistoryRepository extends JpaRepository<WalletHistory, Long> {

    Page<WalletHistory> findByUserId(Long id, Pageable pageable);
}
