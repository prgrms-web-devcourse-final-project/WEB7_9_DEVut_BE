package devut.buzzerbidder.domain.delayedbid.repository;

import devut.buzzerbidder.domain.delayedbid.entity.DelayedBidLog;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DelayedBidRepository extends JpaRepository<DelayedBidLog, Long> {

    boolean existsByDelayedItem(DelayedItem delayedItem);
}
