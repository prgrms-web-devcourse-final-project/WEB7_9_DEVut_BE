package devut.buzzerbidder.domain.likelive.repository;

import devut.buzzerbidder.domain.likelive.entity.LikeLive;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeLiveRepository extends JpaRepository<LikeLive, Long> {

    Optional<LikeLive> findByUserIdAndLiveItemId(Long userId, Long liveItemId);

    long countByLiveItemId(Long liveItemId);
}