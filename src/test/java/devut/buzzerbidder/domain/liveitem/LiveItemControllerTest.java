package devut.buzzerbidder.domain.liveitem;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.user.dto.request.EmailSignUpRequest;
import devut.buzzerbidder.domain.user.entity.Provider;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.entity.User.UserRole;
import devut.buzzerbidder.domain.user.repository.ProviderRepository;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.domain.user.service.RefreshTokenService;
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
import java.time.LocalDateTime;
import java.time.LocalTime;


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
    private ProviderRepository providerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private EmailSignUpRequest signUpRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("password123!"))
                .nickname("hong123")
                .profileImageUrl("https://example.com/image.jpg")
                .role(UserRole.USER)
                .build();

        testUser = userRepository.save(testUser);

        // EMAIL Provider 생성 (이메일 로그인을 위해 필요)
        Provider provider = Provider.builder()
                .providerType(Provider.ProviderType.EMAIL)
                .providerId("test@example.com") // EMAIL의 경우 email을 providerId로 사용
                .user(testUser)
                .build();
        providerRepository.save(provider);

        accessToken = authTokenService.genAccessToken(testUser);
    }

    @Test
    @DisplayName("경매글 생성 성공")
    void createLiveItem() throws Exception {
        // 현재 시간보다 최소 1시간 이후, 09:00~23:00 사이, 30분 단위로 설정
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime liveTime = now.plusHours(2);
        
        // 시간이 09:00~23:00 범위를 벗어나면 조정
        LocalTime timeOnly = liveTime.toLocalTime();
        if (timeOnly.isBefore(LocalTime.of(9, 0))) {
            liveTime = liveTime.withHour(9).withMinute(0).withSecond(0).withNano(0);
        } else if (timeOnly.isAfter(LocalTime.of(23, 0))) {
            liveTime = liveTime.plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        } else {
            // 30분 단위로 조정 (00분 또는 30분)
            int minute = liveTime.getMinute();
            if (minute < 30) {
                liveTime = liveTime.withMinute(0).withSecond(0).withNano(0);
            } else {
                liveTime = liveTime.withMinute(30).withSecond(0).withNano(0);
            }
        }
        
        String liveTimeStr = liveTime.toString();
        
        String requestBody = String.format("""
            {
               "name": "맥북 프로 16인치 2021",
               "category": "ELECTRONICS",
               "itemStatus": "NEW",
               "description": "상태 아주 좋습니다. 배터리 사이클 120회.",
               "initPrice": 1200000,
               "deliveryInclude": true,
               "liveTime": "%s",
               "directDealAvailable": false,
               "region": "서울특별시 강남구",
               "preferredPlace": "강남역 11번 출구",
               "images": [
                 "https://example.com/mac1.jpg",
                 "https://example.com/mac2.jpg"
               ]
             }
            """, liveTimeStr);

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
            .andExpect(jsonPath("$.data.image").exists())
            .andExpect(jsonPath("$.data.liveTime").exists());
    }

    @Test
    @DisplayName("경매글 생성 실패 - 이름없음")
    void createLiveItemNoName() throws Exception {
        // 현재 시간보다 최소 1시간 이후, 09:00~23:00 사이, 30분 단위로 설정
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime liveTime = now.plusHours(2);
        
        // 시간이 09:00~23:00 범위를 벗어나면 조정
        LocalTime timeOnly = liveTime.toLocalTime();
        if (timeOnly.isBefore(LocalTime.of(9, 0))) {
            liveTime = liveTime.withHour(9).withMinute(0).withSecond(0).withNano(0);
        } else if (timeOnly.isAfter(LocalTime.of(23, 0))) {
            liveTime = liveTime.plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        } else {
            // 30분 단위로 조정 (00분 또는 30분)
            int minute = liveTime.getMinute();
            if (minute < 30) {
                liveTime = liveTime.withMinute(0).withSecond(0).withNano(0);
            } else {
                liveTime = liveTime.withMinute(30).withSecond(0).withNano(0);
            }
        }
        
        String liveTimeStr = liveTime.toString();
        
        String requestBody = String.format("""
            {
               "name": "",
               "category": "ELECTRONICS",
               "itemStatus": "NEW",
               "description": "상태 아주 좋습니다. 배터리 사이클 120회.",
               "initPrice": 1200000,
               "deliveryInclude": true,
               "liveTime": "%s",
               "directDealAvailable": false,
               "region": "서울특별시 강남구",
               "preferredPlace": "강남역 11번 출구",
               "images": [
                 "https://example.com/mac1.jpg",
                 "https://example.com/mac2.jpg"
               ]
             }
            """, liveTimeStr);

        ResultActions result = mockMvc.perform(
            post("/api/v1/auction/live")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ).andDo(print());

        result
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").exists());
    }

}
