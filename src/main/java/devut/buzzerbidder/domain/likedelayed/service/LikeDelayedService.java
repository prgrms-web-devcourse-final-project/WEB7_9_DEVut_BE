package devut.buzzerbidder.domain.likedelayed.service;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.likedelayed.entity.LikeDelayed;
import devut.buzzerbidder.domain.likedelayed.repository.LikeDelayedRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeDelayedService {

    private final LikeDelayedRepository likeDelayedRepository;
    private final UserService userService;
    private final DelayedItemRepository delayedItemRepository;

    @Transactional
    public boolean toggleLike(Long userId, Long delayedItemId) {

        User user = userService.findById(userId);

        DelayedItem delayedItem = delayedItemRepository.findById(delayedItemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        Optional<LikeDelayed> existingLike = likeDelayedRepository.findByUserAndDelayedItem(user, delayedItem);

        if (existingLike.isPresent()) {
            likeDelayedRepository.delete(existingLike.get());
            return false;
        } else {
            likeDelayedRepository.save(new LikeDelayed(user, delayedItem));
            return true;
        }
    }

    public long countByDelayedItemId(Long id) {
        return likeDelayedRepository.countByDelayedItemId(id);
    }

    public Set<Long> findLikeDelayedItemIdsByUserId(Long userId) {
        List<Long> ids = likeDelayedRepository.findDelayedItemIdsByUserId(userId);
        return new HashSet<>(ids);
    }

}
