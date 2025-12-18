package devut.buzzerbidder.domain.wallet.repository;

import devut.buzzerbidder.domain.wallet.entity.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

}
