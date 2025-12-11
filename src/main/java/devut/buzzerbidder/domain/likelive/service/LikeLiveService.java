package devut.buzzerbidder.domain.likelive.service;

import devut.buzzerbidder.domain.likelive.entity.LikeLive;
import devut.buzzerbidder.domain.likelive.repository.LikeLiveRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeLiveService {

    private final LikeLiveRepository likeLiveRepository;

    @Transactional
    public boolean toggleLike(Long memberId,Long liveItemId) {

        Optional<LikeLive> existingLike = likeLiveRepository.findByMemberIdAndLiveItemId(memberId, liveItemId);

        if (existingLike.isPresent()) {
            likeLiveRepository.delete(existingLike.get());
            return false;
        } else {
            likeLiveRepository.save(new LikeLive(memberId, liveItemId));
            return true;
        }

    }

    public long countByLiveItemId(Long id) {
        return likeLiveRepository.countByLiveItemId(id);
    }
}
