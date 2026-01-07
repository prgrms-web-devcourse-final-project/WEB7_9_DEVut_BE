package devut.buzzerbidder.domain.liveBid.repository;

import devut.buzzerbidder.domain.liveBid.entity.LiveBidLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveBidLogRepository extends JpaRepository<LiveBidLog, Long> {
}
