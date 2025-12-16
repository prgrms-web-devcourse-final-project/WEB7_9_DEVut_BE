package devut.buzzerbidder.domain.payment.repository;

import devut.buzzerbidder.domain.payment.entity.Withdraw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawRepository extends JpaRepository<Withdraw, Long> {

}
