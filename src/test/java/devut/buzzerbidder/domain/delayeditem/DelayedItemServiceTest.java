package devut.buzzerbidder.domain.delayeditem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;

import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.delayedbid.entity.DelayedBidLog;
import devut.buzzerbidder.domain.delayedbid.repository.DelayedBidRepository;
import devut.buzzerbidder.domain.delayeditem.dto.request.DelayedItemCreateRequest;
import devut.buzzerbidder.domain.delayeditem.dto.request.DelayedItemModifyRequest;
import devut.buzzerbidder.domain.delayeditem.dto.request.DelayedItemSearchRequest;
import devut.buzzerbidder.domain.delayeditem.dto.response.DelayedItemDetailResponse;
import devut.buzzerbidder.domain.delayeditem.dto.response.DelayedItemListResponse;
import devut.buzzerbidder.domain.delayeditem.dto.response.DelayedItemResponse;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.Category;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.ItemStatus;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.delayeditem.service.DelayedItemService;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.entity.User.UserRole;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.wallet.repository.WalletRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.image.ImageService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@Transactional
public class DelayedItemServiceTest {

    @Autowired
    private DelayedItemService delayedItemService;

    @Autowired
    private DelayedItemRepository delayedItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DelayedBidRepository delayedBidRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private ImageService imageService;

    private User seller;
    private User otherUser;
    private List<String> imageUrls;

    @BeforeEach
    void setUp() {
        doNothing().when(imageService).deleteFiles(anyList());

        seller = User.builder()
            .email("seller@example.com")
            .password(passwordEncoder.encode("password123!"))
            .nickname("seller")
            .profileImageUrl("https://example.com/seller.jpg")
            .role(UserRole.USER)
            .build();
        seller = userRepository.save(seller);

        otherUser = User.builder()
            .email("other@example.com")
            .password(passwordEncoder.encode("password123!"))
            .nickname("other")
            .profileImageUrl("https://example.com/other.jpg")
            .role(UserRole.USER)
            .build();
        otherUser = userRepository.save(otherUser);

        imageUrls = Arrays.asList(
            "https://example.com/image1.jpg",
            "https://example.com/image2.jpg"
        );
    }

    @Test
    @DisplayName("경매 물품 생성 성공")
    void t1() {
        // given
        LocalDateTime expectedEndTime = LocalDateTime.now().plusDays(5);

        DelayedItemCreateRequest request = new DelayedItemCreateRequest(
            "테스트 상품",
            Category.ELECTRONICS,
            "상품 설명",
            10000L,
            null,
            expectedEndTime,
            ItemStatus.USED_LIKE_NEW,
            true,
            true,
            "서울시",
            "강남역",
            imageUrls
        );

        // when
        DelayedItemResponse response = delayedItemService.writeDelayedItem(request, seller);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("테스트 상품");
        assertThat(response.currentPrice()).isEqualTo(10000L);
        assertThat(response.endTime()).isEqualTo(expectedEndTime);

        DelayedItem saved = delayedItemRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("테스트 상품");
        assertThat(saved.getImages()).hasSize(2);
    }

    @Test
    @DisplayName("경매 물품 생성 실패 - 종료 시간이 3일 미만")
    void t2() {
        // given
        DelayedItemCreateRequest request = new DelayedItemCreateRequest(
            "테스트 상품",
            Category.ELECTRONICS,
            "상품 설명",
            10000L,
            null,
            LocalDateTime.now().plusDays(2),
            ItemStatus.USED_LIKE_NEW,
            true,
            true,
            "서울시",
            "강남역",
            imageUrls
        );

        // when & then
        assertThatThrownBy(() -> delayedItemService.writeDelayedItem(request, seller))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.INVALID_END_TIME.getMessage());
    }

    @Test
    @DisplayName("경매 물품 생성 실패 - 이미지 없음")
    void t3() {
        // given
        DelayedItemCreateRequest request = new DelayedItemCreateRequest(
            "테스트 상품",
            Category.ELECTRONICS,
            "상품 섦명",
            10000L,
            null,
            LocalDateTime.now().plusDays(5),
            ItemStatus.USED_LIKE_NEW,
            true,
            true,
            "서울시",
            "강남역",
            null
        );

        // when & then
        assertThatThrownBy(() -> delayedItemService.writeDelayedItem(request, seller))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.IMAGE_FILE_EMPTY.getMessage());
    }

    @Test
    @DisplayName("경매 물품 수정 성공")
    void t4() {
        // given
        DelayedItemCreateRequest createRequest = new DelayedItemCreateRequest(
            "테스트 상품",
            Category.ELECTRONICS,
            "상품 설명",
            10000L,
            null,
            LocalDateTime.now().plusDays(5),
            ItemStatus.USED_LIKE_NEW,
            true,
            true,
            "서울시",
            "강남역",
            imageUrls
        );
        DelayedItemResponse created = delayedItemService.writeDelayedItem(createRequest, seller);

        LocalDateTime expectedEndTime = LocalDateTime.now().plusDays(7);

        DelayedItemModifyRequest modifyRequest = new DelayedItemModifyRequest(
            "수정된 상품",
            Category.CLOTHES,
            "수정된 설명",
            15000L,
            null,
            expectedEndTime,
            ItemStatus.NEW,
            false,
            false,
            "부산시",
            "해운대",
            imageUrls
        );

        // when
        DelayedItemResponse response = delayedItemService.modifyDelayedItem(
            created.id(),
            modifyRequest,
            seller
        );

        // then
        assertThat(response.name()).isEqualTo("수정된 상품");
        assertThat(response.currentPrice()).isEqualTo(15000L);
        assertThat(response.endTime()).isEqualTo(expectedEndTime);
    }

    @Test
    @DisplayName("경매 물품 수정 실패 - 작성자가 아님")
    void t5() {
        // given
        DelayedItemCreateRequest createRequest = new DelayedItemCreateRequest(
            "테스트 상품",
            Category.ELECTRONICS,
            "상품 설명",
            10000L,
            null,
            LocalDateTime.now().plusDays(5),
            ItemStatus.USED_LIKE_NEW,
            true,
            true,
            "서울시",
            "강남역",
            imageUrls
        );
        DelayedItemResponse created = delayedItemService.writeDelayedItem(createRequest, seller);

        DelayedItemModifyRequest modifyRequest = new DelayedItemModifyRequest(
            "수정된 상품",
            Category.CLOTHES,
            "수정된 설명",
            15000L,
            null,
            LocalDateTime.now().plusDays(7),
            ItemStatus.NEW,
            false,
            false,
            "부산시",
            "해운대",
            imageUrls
        );

        // when & then
        assertThatThrownBy(() ->
            delayedItemService.modifyDelayedItem(created.id(), modifyRequest, otherUser))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.FORBIDDEN_ACCESS.getMessage());
    }

    @Test
    @DisplayName("경매 물품 수정 실패 - 입찰이 있는 경우")
    void t6() {
        // given
        DelayedItemCreateRequest createRequest = new DelayedItemCreateRequest(
            "테스트 상품",
            Category.ELECTRONICS,
            "상품 설명",
            10000L,
            null,
            LocalDateTime.now().plusDays(5),
            ItemStatus.USED_LIKE_NEW,
            true,
            true,
            "서울시",
            "강남역",
            imageUrls
        );
        DelayedItemResponse created = delayedItemService.writeDelayedItem(createRequest, seller);

        // 입찰 생성
        DelayedItem item = delayedItemRepository.findById(created.id()).orElseThrow();
        DelayedBidLog bid = DelayedBidLog.builder()
            .delayedItem(item)
            .bidderUserId(otherUser.getId())
            .bidAmount(15000L)
            .bidTime(LocalDateTime.now())
            .build();
        delayedBidRepository.save(bid);

        DelayedItemModifyRequest modifyRequest = new DelayedItemModifyRequest(
            "수정된 상품",
            Category.CLOTHES,
            "수정된 설명",
            15000L,
            null,
            LocalDateTime.now().plusDays(7),
            ItemStatus.NEW,
            false,
            false,
            "부산시",
            "해운대",
            imageUrls
        );

        // when & then
        assertThatThrownBy(() ->
            delayedItemService.modifyDelayedItem(created.id(), modifyRequest, seller))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.EDIT_UNAVAILABLE_DUE_TO_BIDS.getMessage());
    }

    @Test
    @DisplayName("경매 물품 삭제 성공")
    void t7() {
        // given
        DelayedItemCreateRequest request = new DelayedItemCreateRequest(
            "테스트 상품",
            Category.ELECTRONICS,
            "상품 설명",
            10000L,
            null,
            LocalDateTime.now().plusDays(5),
            ItemStatus.USED_LIKE_NEW,
            true,
            true,
            "서울시",
            "강남역",
            imageUrls
        );
        DelayedItemResponse created = delayedItemService.writeDelayedItem(request, seller);

        // when
        delayedItemService.deleteDelayedItem(created.id(), seller);

        // then
        assertThat(delayedItemRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("경매 물품 삭제 실패 - 작성자가 아님")
    void t8() {
        DelayedItemCreateRequest request = new DelayedItemCreateRequest(
            "테스트 상품",
            Category.ELECTRONICS,
            "상품 설명",
            10000L,
            null,
            LocalDateTime.now().plusDays(5),
            ItemStatus.USED_LIKE_NEW,
            true,
            true,
            "서울시",
            "강남역",
            imageUrls
        );
        DelayedItemResponse created = delayedItemService.writeDelayedItem(request, seller);

        // when & then
        assertThatThrownBy(() ->
            delayedItemService.deleteDelayedItem(created.id(), otherUser))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.FORBIDDEN_ACCESS.getMessage());

        assertThat(delayedItemRepository.findById(created.id())).isPresent();
    }

    @Test
    @DisplayName("경매 물품 삭제 실패 - 입찰이 있는 경우")
    void t9() {
        // given
        DelayedItemCreateRequest request = new DelayedItemCreateRequest(
            "테스트 상품",
            Category.ELECTRONICS,
            "상품 설명",
            10000L,
            null,
            LocalDateTime.now().plusDays(5),
            ItemStatus.USED_LIKE_NEW,
            true,
            true,
            "서울시",
            "강남역",
            imageUrls
        );
        DelayedItemResponse created = delayedItemService.writeDelayedItem(request, seller);

        DelayedItem item = delayedItemRepository.findById(created.id()).orElseThrow();
        DelayedBidLog bid = DelayedBidLog.builder()
            .delayedItem(item)
            .bidderUserId(otherUser.getId())
            .bidAmount(15000L)
            .bidTime(LocalDateTime.now())
            .build();
        delayedBidRepository.save(bid);

        // when & then
        assertThatThrownBy(() ->
            delayedItemService.deleteDelayedItem(created.id(), seller))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.DELETE_UNAVAILABLE_DUE_TO_BIDS.getMessage());

        assertThat(delayedItemRepository.findById(created.id())).isPresent();
    }

    @Test
    @DisplayName("경매 물품 상세 조회 성공")
    void t10() {
        // given
        DelayedItemCreateRequest request = new DelayedItemCreateRequest(
            "테스트 상품",
            Category.ELECTRONICS,
            "상품 설명",
            10000L,
            null,
            LocalDateTime.now().plusDays(5),
            ItemStatus.USED_LIKE_NEW,
            true,
            true,
            "서울시",
            "강남역",
            imageUrls
        );
        DelayedItemResponse created = delayedItemService.writeDelayedItem(request, seller);

        // when
        DelayedItemDetailResponse response = delayedItemService.getDelayedItem(created.id());

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(created.id());
        assertThat(response.name()).isEqualTo("테스트 상품");
        assertThat(response.category()).isEqualTo(Category.ELECTRONICS);
        assertThat(response.images()).hasSize(2);
        assertThat(response.likeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("경매 물품 상세 조회 실패 - 존재하지 않는 물품")
    void t11() {
        // when & then
        assertThatThrownBy(() -> delayedItemService.getDelayedItem(99999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(ErrorCode.NOT_FOUND_DATA.getMessage());
    }

    @Test
    @DisplayName("경매 물품 목록 조회 성공")
    void t12() {
        // given
        DelayedItemCreateRequest request1 = new DelayedItemCreateRequest(
            "전자제품 상품",
            Category.ELECTRONICS,
            "상품 설명",
            10000L,
            null,
            LocalDateTime.now().plusDays(5),
            ItemStatus.USED_LIKE_NEW,
            true,
            true,
            "서울시",
            "강남역",
            imageUrls
        );
        delayedItemService.writeDelayedItem(request1, seller);

        DelayedItemCreateRequest request2 = new DelayedItemCreateRequest(
            "패션 상품",
            Category.CLOTHES,
            "상품 설명",
            20000L,
            null,
            LocalDateTime.now().plusDays(5),
            ItemStatus.NEW,
            true,
            true,
            "서울시",
            "강남역",
            imageUrls
        );
        delayedItemService.writeDelayedItem(request2, seller);

        DelayedItemSearchRequest searchRequest = new DelayedItemSearchRequest(
            null,
            Category.ELECTRONICS,
            null,
            null
        );
        Pageable pageable = PageRequest.of(0, 10);

        // when
        DelayedItemListResponse response = delayedItemService.getDelayedItems(
            searchRequest,
            pageable
        );

        // then
        assertThat(response.delayedItems()).hasSizeGreaterThan(1);
        assertThat(response.delayedItems()).extracting("name").contains("전자제품 상품");
    }

    @Test
    @DisplayName("인기 경매 물품 조회 성공")
    void t13() {
        // given
        DelayedItemCreateRequest request = new DelayedItemCreateRequest(
            "테스트 상품",
            Category.ELECTRONICS,
            "상품 설명",
            10000L,
            null,
            LocalDateTime.now().plusDays(5),
            ItemStatus.USED_LIKE_NEW,
            true,
            true,
            "서울시",
            "강남역",
            imageUrls
        );
        delayedItemService.writeDelayedItem(request, seller);

        // when
        DelayedItemListResponse response = delayedItemService.getHotDelayedItems(10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.delayedItems()).isNotNull();
    }
}