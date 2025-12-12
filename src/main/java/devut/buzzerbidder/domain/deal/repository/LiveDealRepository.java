package devut.buzzerbidder.domain.deal.repository;

import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveDealRepository extends JpaRepository<LiveDeal, Long> {

}
