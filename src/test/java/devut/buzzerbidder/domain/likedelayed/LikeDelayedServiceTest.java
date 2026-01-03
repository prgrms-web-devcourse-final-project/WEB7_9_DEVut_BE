package devut.buzzerbidder.domain.likedelayed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.AuctionStatus;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.Category;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.ItemStatus;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.likedelayed.service.LikeDelayedService;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.entity.User.UserRole;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@Transactional
public class LikeDelayedServiceTest {

    @Autowired
    private LikeDelayedService likeDelayedService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DelayedItemRepository delayedItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user1;
    private User user2;
    private DelayedItem delayedItem;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
            .email("user1@example.com")
            .password(passwordEncoder.encode("password123!"))
            .nickname("user1")
            .profileImageUrl("https://example.com/user1.jpg")
            .role(UserRole.USER)
            .build();
        user1 = userRepository.save(user1);

        user2 = User.builder()
            .email("user2@example.com")
            .password(passwordEncoder.encode("password123!"))
            .nickname("user2")
            .profileImageUrl("https://example.com/user2.jpg")
            .role(UserRole.USER)
            .build();
        user2 = userRepository.save(user2);

        delayedItem = DelayedItem.builder()
            .sellerUserId(user1.getId())
            .name("테스트 상품")
            .category(Category.ELECTRONICS)
            .description("상품 설명")
            .startPrice(10000L)
            .currentPrice(10000L)
            .endTime(LocalDateTime.now().plusDays(5))
            .itemStatus(ItemStatus.USED_LIKE_NEW)
            .auctionStatus(AuctionStatus.BEFORE_BIDDING)
            .deliveryInclude(true)
            .directDealAvailable(true)
            .region("서울시")
            .preferredPlace("강남역")
            .build();
        delayedItem = delayedItemRepository.save(delayedItem);
    }

    @Test
    @DisplayName("찜 추가 성공")
    void t1() {
        // when
        boolean result = likeDelayedService.toggleLike(user2.getId(), delayedItem.getId());

        // then
        assertThat(result).isTrue();
        assertThat(likeDelayedService.countByDelayedItemId(delayedItem.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("찜 제거 성공")
    void t2() {
        // given
        likeDelayedService.toggleLike(user2.getId(), delayedItem.getId());

        // when
        boolean result = likeDelayedService.toggleLike(user2.getId(), delayedItem.getId());

        // then
        assertThat(result).isFalse();
        assertThat(likeDelayedService.countByDelayedItemId(delayedItem.getId())).isEqualTo(0L);
    }

    @Test
    @DisplayName("찜 실패 - 존재하지 않는 경매 물품")
    void t3() {
        // when & then
        assertThatThrownBy(() ->
            likeDelayedService.toggleLike(user2.getId(), 99999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.NOT_FOUND_DATA.getMessage());
    }

    @Test
    @DisplayName("여러 사용자의 좋아요 카운트")
    void t4() {
        // given
        likeDelayedService.toggleLike(user1.getId(), delayedItem.getId());
        likeDelayedService.toggleLike(user2.getId(), delayedItem.getId());

        // when
        long count = likeDelayedService.countByDelayedItemId(delayedItem.getId());

        // then
        assertThat(count).isEqualTo(2L);
    }
}
