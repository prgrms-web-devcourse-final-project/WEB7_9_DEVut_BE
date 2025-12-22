package devut.buzzerbidder.domain.deal.repository;

import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DelayedDealRepository extends JpaRepository<DelayedDeal, Long> {

    // DelayedItem으로 Deal 조회 (중복 생성 방지)
    Optional<DelayedDeal> findByItem(DelayedItem item);

}
