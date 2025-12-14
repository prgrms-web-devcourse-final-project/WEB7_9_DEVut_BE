package devut.buzzerbidder.domain.likelive.service;

import devut.buzzerbidder.domain.likelive.entity.LikeLive;
import devut.buzzerbidder.domain.likelive.repository.LikeLiveRepository;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeLiveService {

    private final LikeLiveRepository likeLiveRepository;
    private final UserService userService;
    private final LiveItemRepository liveItemRepository;

    @Transactional
    public boolean toggleLike(Long userId,Long liveItemId) {

        User user = userService.findById(userId);

        LiveItem liveItem = liveItemRepository.findById(liveItemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        Optional<LikeLive> existingLike = likeLiveRepository.findByUserAndLiveItem(user, liveItem);

        if (existingLike.isPresent()) {
            likeLiveRepository.delete(existingLike.get());
            return false;
        } else {
            likeLiveRepository.save(new LikeLive(user, liveItem));
            return true;
        }

    }

    public long countByLiveItemId(Long id) {
        return likeLiveRepository.countByLiveItemId(id);
    }
}