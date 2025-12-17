package devut.buzzerbidder.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.Carrier;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.deliveryTracking.dto.request.DeliveryRequest;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.dto.request.UserUpdateRequest;
import devut.buzzerbidder.domain.user.entity.Provider;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.ProviderRepository;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.util.UserTestUtil;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class UserMeControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LiveDealRepository liveDealRepository;
    @Autowired
    private UserTestUtil userTestUtil;
    @Autowired
    private LiveItemRepository liveItemRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ProviderRepository providerRepository;

    private User user1;
    private User user2;
    private String user1Token;
    private String user2Token;

    private Long liveDeal1Id;
    private Long liveDeal2Id;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // 각 테스트 전에 데이터 정리
        liveDealRepository.deleteAll();
        liveDealRepository.flush();
        liveItemRepository.deleteAll();
        liveItemRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();

        user1Token = userTestUtil.createUserAndGetToken("new@user.com", "asdf1234!", "gildong", null);;
        user2Token = userTestUtil.createUserAndGetToken("new2@user.com", "asdf1234!", "gilddong", null);
        user1 = userRepository.findByEmail("new@user.com").orElseThrow();
        user2 = userRepository.findByEmail("new2@user.com").orElseThrow();

        LiveItem liveItem1 = LiveItem.builder()
                .sellerUserId(user1.getId())
                .auctionRoom(null)
                .name("Sample Live Item")
                .category(LiveItem.Category.ELECTRONICS)
                .description("샘플 설명입니다.")
                .initPrice(1000000L)
                .deliveryInclude(false)
                .itemStatus(LiveItem.ItemStatus.NEW)
                .auctionStatus(LiveItem.AuctionStatus.BEFORE_BIDDING)
                .liveTime(LocalDateTime.now().plusDays(3))
                .directDealAvailable(false)
                .region("서울시 강남구 역삼동")
                .preferredPlace("역삼역 근처 카페")
                .build();

        liveItemRepository.save(liveItem1);

        LiveItem liveItem2 = LiveItem.builder()
                .sellerUserId(user1.getId())
                .auctionRoom(null)
                .name("Sample Live Item")
                .category(LiveItem.Category.ELECTRONICS)
                .description("샘플 설명입니다.")
                .initPrice(1000000L)
                .deliveryInclude(false)
                .itemStatus(LiveItem.ItemStatus.NEW)
                .auctionStatus(LiveItem.AuctionStatus.BEFORE_BIDDING)
                .liveTime(LocalDateTime.now().plusDays(3))
                .directDealAvailable(false)
                .region("서울시 강남구 역삼동")
                .preferredPlace("역삼역 근처 카페")
                .build();

        liveItemRepository.save(liveItem2);

        LiveDeal deal1 = LiveDeal.builder()
                .item(liveItem1)
                .buyer(user1)
                .winningPrice(100000L)
                .status(DealStatus.PENDING)
                .build();
        liveDealRepository.save(deal1);
        liveDeal1Id = deal1.getId();

        LiveDeal deal2 = LiveDeal.builder()
                .item(liveItem2)
                .buyer(user1)
                .winningPrice(100000L)
                .status(DealStatus.PENDING)
                .carrier(Carrier.CJ_LOGISTICS)
                .trackingNumber("1234567890")
                .build();
        liveDealRepository.save(deal2);
        liveDeal2Id = deal2.getId();
    }

    @Nested
    @DisplayName("배송정보 입력 테스트")
    class t1 {

        private final DeliveryRequest deliveryRequest = new DeliveryRequest(
                "kr.cjlogistics",
                "1234567890"
        );

        @Test
        @DisplayName("라이브 - 거래 배송정보 입력 성공")
        void live_enterDeliveryInfo_success() throws Exception {
            // given
            String requestBody = objectMapper.writeValueAsString(deliveryRequest);
            String auctionType = "live";
            Long testLiveDealId = liveDeal1Id;

            // when & then
            mockMvc.perform(patch("/api/v1/users/me/deals/%s/%d/delivery".formatted(auctionType, testLiveDealId))
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.msg").value("배송 정보가 입력되었습니다."))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

            // 배송정보가 잘 들어갔는지 확인
            LiveDeal deal = liveDealRepository.findById(testLiveDealId).orElseThrow();
            assertThat(deal.getCarrier()).isEqualTo(Carrier.CJ_LOGISTICS);
        }

        @Test
        @DisplayName("라이브 - 거래 배송정보 입력 실패: 존재하지 않는 거래")
        void live_enterDeliveryInfo_fail_dealNotFound() throws Exception {
            // given
            String requestBody = objectMapper.writeValueAsString(deliveryRequest);
            String auctionType = "live";
            Long testLiveDealId = liveDeal1Id + 9999;

            // when & then
            mockMvc.perform(patch("/api/v1/users/me/deals/%s/%d/delivery".formatted(auctionType, testLiveDealId))
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.resultCode").value("D001"))
                    .andExpect(jsonPath("$.msg").value("존재하지 않는 거래입니다."))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

            System.out.println(liveDealRepository.count());
        }

        @Test
        @DisplayName("라이브 - 거래 배송정보 입력 실패: 존재하지 않는 거래")
        void live_enterDeliveryInfo_fail_dealNotFoun1d() throws Exception {
            // given
            String requestBody = objectMapper.writeValueAsString(deliveryRequest);
            String auctionType = "live";
            Long testLiveDealId = liveDeal1Id + 9999;

            // when & then
            mockMvc.perform(patch("/api/v1/users/me/deals/%s/%d/delivery".formatted(auctionType, testLiveDealId))
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.resultCode").value("D001"))
                    .andExpect(jsonPath("$.msg").value("존재하지 않는 거래입니다."))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

            System.out.println(liveDealRepository.count());
        }

        @Test
        @DisplayName("라이브 - 거래 배송정보 입력 실패: 잘못된 경매 타입")
        void live_enterDeliveryInfo_fail_invalidType() throws Exception {
            // given
            String requestBody = objectMapper.writeValueAsString(deliveryRequest);
            String auctionType = "liveee";
            Long testLiveDealId = liveDeal1Id;

            // when & then
            mockMvc.perform(patch("/api/v1/users/me/deals/%s/%d/delivery".formatted(auctionType, testLiveDealId))
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.resultCode").value("D002"))
                    .andExpect(jsonPath("$.msg").value("잘못된 경매 유형입니다."))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

            System.out.println(liveDealRepository.count());
        }
    }

    @Nested
    @DisplayName("배송조회 테스트")
    class t2 {
        @Test
        @DisplayName("라이브 - 배송조회 성공")
        void live_deliveryTracking_success() throws Exception {
            // given
            String auctionType = "live";
            Long testLiveDealId = liveDeal2Id;

            // when & then
            mockMvc.perform(get("/api/v1/users/me/deals/%s/%d/delivery".formatted(auctionType, testLiveDealId))
                            .header("Authorization", "Bearer " + user1Token))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.msg").value("배송조회 성공"))
                    .andExpect(jsonPath("$.data.lastEvent.time").value("2025-11-04T12:22:09.000+09:00"))
                    .andExpect(jsonPath("$.data.lastEvent.status").value("배송완료"))
                    .andExpect(jsonPath("$.data.lastEvent.locationName").value("서울목동중앙"))
                    .andExpect(jsonPath("$.data.lastEvent.description").value("고객님의 상품이 배송완료 되었습니다.(담당사원:우성환 010-7566-9558)"))
                    .andExpect(jsonPath("$.data.events").exists());
        }

        @Test
        @DisplayName("라이브 - 배송조회 실패: 존재하지 않는 거래")
        void live_deliveryTracking_fail_noDeal() throws Exception {
            // given
            String auctionType = "live";
            Long testLiveDealId = liveDeal1Id +9999;

            // when & then
            mockMvc.perform(get("/api/v1/users/me/deals/%s/%d/delivery".formatted(auctionType, testLiveDealId))
                            .header("Authorization", "Bearer " + user1Token))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.resultCode").value("D001"))
                    .andExpect(jsonPath("$.msg").value("존재하지 않는 거래입니다."))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        }

        @Test
        @DisplayName("라이브 - 배송조회 실패: 잘못된 경매 타입")
        void live_deliveryTracking_fail_invalidType() throws Exception {
            // given
            String auctionType = "livee";
            Long testLiveDealId = liveDeal1Id;

            // when & then
            mockMvc.perform(get("/api/v1/users/me/deals/%s/%d/delivery".formatted(auctionType, testLiveDealId))
                            .header("Authorization", "Bearer " + user1Token))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.resultCode").value("D002"))
                    .andExpect(jsonPath("$.msg").value("잘못된 경매 유형입니다."))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        }
    }

    @Nested
    @DisplayName("내 정보")
    class t3 {

        @Test
        @DisplayName("내 정보 조회 성공")
        void getMyProfile_success() throws Exception {
            mockMvc.perform(get("/api/v1/users/me")
                            .header("Authorization", "Bearer " + user1Token))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.msg").value("회원정보 조회 성공"))
                    .andExpect(jsonPath("$.data.id").value(user1.getId()))
                    .andExpect(jsonPath("$.data.email").value("new@user.com"))
                    .andExpect(jsonPath("$.data.nickname").value("gildong"))
                    .andExpect(jsonPath("$.data.image").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.data.createDate").exists())
                    .andExpect(jsonPath("$.data.modifyDate").exists())
                    .andExpect(jsonPath("$.data.bizz").value(0L));
        }

        @Test
        @DisplayName("내 정보 조회 API 실패 - 인증 없음")
        void getMyProfile_Fail_Unauthorized() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/users/me"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }


        @Test
        @DisplayName("내 정보 수정 API 성공")
        void updateMyProfile_Success() throws Exception {
            // given
            User user = user1;
            String accessToken = user1Token;

            UserUpdateRequest updateRequest = new UserUpdateRequest(
                    "updated@example.com",
                    "updatedNickname",
                    "https://example.com/updated.jpg"
            );
            String requestBody = objectMapper.writeValueAsString(updateRequest);

            // when & then
            mockMvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.msg").value("회원정보 수정 성공"))
                    .andExpect(jsonPath("$.data.email").value("updated@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("updatedNickname"))
                    .andExpect(jsonPath("$.data.image").value("https://example.com/updated.jpg"));

            // DB에서 실제로 수정되었는지 확인
            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assert updatedUser.getEmail().equals("updated@example.com");
            assert updatedUser.getNickname().equals("updatedNickname");
        }

        @Test
        @DisplayName("내 정보 수정 API 성공 - 부분 수정 (nickname만)")
        void updateMyProfile_Success_PartialUpdate() throws Exception {
            // given
            String accessToken = user1Token;

            UserUpdateRequest updateRequest = new UserUpdateRequest(
                    null, // email은 변경하지 않음
                    "newNickname",
                    null  // image는 변경하지 않음
            );
            String requestBody = objectMapper.writeValueAsString(updateRequest);

            // when & then
            mockMvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.data.email").value("new@user.com")) // 기존 값 유지
                    .andExpect(jsonPath("$.data.nickname").value("newNickname")) // 변경됨
                    .andExpect(jsonPath("$.data.image").value(org.hamcrest.Matchers.nullValue()));
        }

        @Test
        @DisplayName("내 정보 수정 API 실패 - 인증 없음")
        void updateMyProfile_Fail_Unauthorized() throws Exception {
            // given
            UserUpdateRequest updateRequest = new UserUpdateRequest(
                    "updated@example.com",
                    "updatedNickname",
                    null
            );
            String requestBody = objectMapper.writeValueAsString(updateRequest);

            // when & then
            mockMvc.perform(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("내 정보 수정 API 실패 - 이메일 중복")
        void updateMyProfile_Fail_DuplicateEmail() throws Exception {
            // given
            String accessToken = user1Token;

            UserUpdateRequest updateRequest = new UserUpdateRequest(
                    "new2@user.com", // 이미 사용 중인 이메일
                    null,
                    null
            );
            String requestBody = objectMapper.writeValueAsString(updateRequest);

            // when & then
            mockMvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.resultCode").value("M002"))
                    .andExpect(jsonPath("$.msg").value("이미 사용 중인 이메일입니다."));
        }

        @Test
        @DisplayName("내 정보 수정 API 실패 - 닉네임 중복")
        void updateMyProfile_Fail_DuplicateNickname() throws Exception {
            // given
            String accessToken = user1Token;

            UserUpdateRequest updateRequest = new UserUpdateRequest(
                    "new@user.com",
                    "gilddong", // 이미 사용 중인 닉네임
                    null
            );
            String requestBody = objectMapper.writeValueAsString(updateRequest);

            // when & then
            mockMvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.resultCode").value("M003"))
                    .andExpect(jsonPath("$.msg").value("이미 사용 중인 닉네임입니다."));
        }

        @Test
        @DisplayName("내 정보 수정 API 실패 - 유효성 검증 실패 (이메일 형식 오류)")
        void updateMyProfile_Fail_InvalidEmail() throws Exception {
            // given
            String accessToken = user1Token;

            UserUpdateRequest updateRequest = new UserUpdateRequest(
                    "invalid-email", // 잘못된 이메일 형식
                    null,
                    null
            );
            String requestBody = objectMapper.writeValueAsString(updateRequest);

            // when & then
            mockMvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("내 정보 수정 API 성공 - 자신의 이메일로 변경 (중복 아님)")
        void updateMyProfile_Success_SameEmail() throws Exception {
            // given
            String accessToken = user1Token;

            UserUpdateRequest updateRequest = new UserUpdateRequest(
                    "new@user.com", // 자신의 기존 이메일
                    "newNickname",
                    null
            );
            String requestBody = objectMapper.writeValueAsString(updateRequest);

            // when & then
            mockMvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.data.email").value("new@user.com"))
                    .andExpect(jsonPath("$.data.nickname").value("newNickname"));
        }

        @Test
        @DisplayName("내 정보 수정 API 성공 - 자신의 닉네임으로 변경 (중복 아님)")
        void updateMyProfile_Success_SameNickname() throws Exception {
            // given
            String accessToken = user1Token;

            UserUpdateRequest updateRequest = new UserUpdateRequest(
                    "updated@example.com",
                    "gildong", // 자신의 기존 닉네임
                    null
            );
            String requestBody = objectMapper.writeValueAsString(updateRequest);

            // when & then
            mockMvc.perform(patch("/api/v1/users/me")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200"))
                    .andExpect(jsonPath("$.data.email").value("updated@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("gildong"));
        }

    }
}
