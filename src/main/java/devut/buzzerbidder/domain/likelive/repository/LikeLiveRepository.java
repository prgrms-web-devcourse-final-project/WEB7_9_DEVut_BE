package devut.buzzerbidder.domain.likelive.repository;

import devut.buzzerbidder.domain.likelive.entity.LikeLive;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeLiveRepository extends JpaRepository<LikeLive, Long> {

    Optional<LikeLive> findByUserAndLiveItem(User user, LiveItem liveItem);

    long countByLiveItemId(Long liveItemId);
}