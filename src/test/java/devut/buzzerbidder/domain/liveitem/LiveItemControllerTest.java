package devut.buzzerbidder.domain.liveitem;

import devut.buzzerbidder.domain.liveitem.dto.request.LiveItemCreateRequest;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.AuctionStatus;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.Category;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.ItemStatus;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.member.entity.Member;
import devut.buzzerbidder.domain.member.entity.Member.MemberRole;
import devut.buzzerbidder.domain.member.entity.Member.ProviderType;
import devut.buzzerbidder.domain.member.repository.MemberRepository;
import devut.buzzerbidder.domain.member.service.AuthTokenService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class LiveItemControllerTest {

    private WebTestClient webTestClient;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private LiveItemRepository liveItemRepository;

    private String accessToken;
    private Member testMember;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {

        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

        Member member = Member.builder()
            .email("test@example.com")          // 필수, unique
            .password("test1234")             // nullable = true (소셜 로그인은 null 가능)
            .name("홍길동")                      // 필수
            .nickname("tester01")               // 필수, unique
            .birthDate(LocalDate.of(1995, 1, 1)) // 필수
            .profileImageUrl(null)               // 선택
            .role(MemberRole.USER)               // null 넣으면 USER로 자동 세팅됨
            .providerType(ProviderType.EMAIL)    // null 넣으면 EMAIL로 자동 세팅됨
            .providerId(null)                    // 선택
            .build();

        testMember = memberRepository.save(member);
        accessToken = authTokenService.genAccessToken(testMember);

        // 테스트용 LiveItem 3개 생성
        List<LiveItemCreateRequest> requests = List.of(
            new LiveItemCreateRequest(1L, "아이템1", Category.ELECTRONICS, "설명1", 1000, ItemStatus.NEW, AuctionStatus.BEFORE_BIDDING,
                LocalDateTime.now().plusDays(1), true, "서울", "강남역", List.of("https://example.com/img1.png")),
            new LiveItemCreateRequest(2L, "아이템2", Category.ELECTRONICS, "설명2", 500, ItemStatus.NEW, AuctionStatus.BEFORE_BIDDING,
                LocalDateTime.now().plusDays(2), false, "부산", "해운대", List.of("https://example.com/img2.png")),
            new LiveItemCreateRequest(3L, "아이템3", Category.ELECTRONICS, "설명3", 2000, ItemStatus.NEW, AuctionStatus.BEFORE_BIDDING,
                LocalDateTime.now().plusDays(3), true, "대구", "동성로", List.of("https://example.com/img3.png"))
        );

        // DB에 저장
        requests.forEach(req -> liveItemRepository.save(new LiveItem(req, testMember)));

    }

    @Test
    @DisplayName("경매글 생성 성공")
    void createLiveItemTest() {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("auctionId", 1L);
        requestBody.put("name", "테스트 아이템4");
        requestBody.put("category", "ELECTRONICS");
        requestBody.put("description", "테스트용 경매품입니다.");
        requestBody.put("initPrice", 10000);
        requestBody.put("itemstatus", "NEW");
        requestBody.put("auctionStatus", "BEFORE_BIDDING");
        requestBody.put("liveDate", "2025-12-10T14:00:00");
        requestBody.put("directDealAvailable", true);
        requestBody.put("region", "서울");
        requestBody.put("preferredPlace", "강남역");
        requestBody.put("images", List.of("https://example.com/image4.png"));

        webTestClient.post().uri("/api/v1/auction/live")
            .header("Authorization", "Bearer " + accessToken)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.msg").isEqualTo("경매품 생성")
            .jsonPath("$.data.name").isEqualTo("테스트 아이템4")
            .jsonPath("$.data.image").isEqualTo("https://example.com/image4.png");
    }


}
