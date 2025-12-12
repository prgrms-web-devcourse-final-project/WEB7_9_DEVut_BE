package devut.buzzerbidder.domain.liveitem;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.user.dto.request.EmailSignUpRequest;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.entity.User.ProviderType;
import devut.buzzerbidder.domain.user.entity.User.UserRole;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.domain.user.service.RefreshTokenService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;


@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@AutoConfigureMockMvc
@Transactional
public class LiveItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String accessToken;
    private User testUser;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private EmailSignUpRequest signUpRequest;

    @BeforeEach
    void setUp() {

        testUser = new User(
            "test@example.com",
            "password123!",
            "홍길동",
            "hong123",
            LocalDate.of(1990, 1, 1),
            "https://example.com/image.jpg",
            UserRole.USER,
            ProviderType.EMAIL,
            null
        );
        userRepository.save(testUser);
        accessToken = authTokenService.genAccessToken(testUser);

    }

    @Test
    @DisplayName("경매글 생성 성공")
    void createLiveItem() throws Exception {
        String requestBody = """
            {
               "auctionId": 1,
               "name": "맥북 프로 16인치 2021",
               "category": "ELECTRONICS",
               "Itemstatus": "NEW",
               "description": "상태 아주 좋습니다. 배터리 사이클 120회.",
               "initPrice": 1200000,
               "deliveryInclude": true,
               "liveDate": "2025-01-15T20:00:00",
               "directDealAvailable": false,
               "region": "서울특별시 강남구",
               "preferredPlace": "강남역 11번 출구",
               "auctionStatus": "BEFORE_BIDDING",
               "images": [
                 "https://example.com/mac1.jpg",
                 "https://example.com/mac2.jpg"
               ]
             }
            """;

        ResultActions result = mockMvc.perform(
            post("/api/v1/auction/live")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ).andDo(print());

        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("경매품 생성"))
            .andExpect(jsonPath("$.data.name").value("맥북 프로 16인치 2021"))
            .andExpect(jsonPath("$.data.image").value("example.jpg"))
            .andExpect(jsonPath("$.data.liveDate").value("2025-01-15T20:00:00"));
    }

    @Test
    @DisplayName("경매글 생성 실패 - 이름없음")
    void createLiveItemNoName() throws Exception {
        String requestBody = """
            {
               "auctionId": 1,
               "name": "",
               "category": "ELECTRONICS",
               "Itemstatus": "NEW",
               "description": "상태 아주 좋습니다. 배터리 사이클 120회.",
               "initPrice": 1200000,
               "deliveryInclude": true,
               "liveDate": "2025-01-15T20:00:00",
               "directDealAvailable": false,
               "region": "서울특별시 강남구",
               "preferredPlace": "강남역 11번 출구",
               "auctionStatus": "BEFORE_BIDDING",
               "images": [
                 "https://example.com/mac1.jpg",
                 "https://example.com/mac2.jpg"
               ]
             }
            """;

        ResultActions result = mockMvc.perform(
            post("/api/v1/auction/live")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ).andDo(print());

        result
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.resultCode").value("CMN002"))
            .andExpect(jsonPath("$.msg").value("서버 내부 오류가 발생했습니다."));
    }

}
