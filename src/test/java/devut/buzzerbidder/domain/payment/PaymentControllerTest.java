package devut.buzzerbidder.domain.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import devut.buzzerbidder.TestcontainersConfig;
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.TossPaymentsClient;
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.dto.response.TossConfirmResponseDto;
import devut.buzzerbidder.domain.payment.repository.PaymentRepository;
import devut.buzzerbidder.domain.user.entity.Provider;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.entity.User.UserRole;
import devut.buzzerbidder.domain.user.repository.ProviderRepository;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@AutoConfigureMockMvc
@Transactional
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String accessToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthTokenService authTokenService;

    @MockBean
    private TossPaymentsClient tossPaymentsClient;

    @BeforeEach
    void setUp() {
        User testUser = User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("password123!"))
                .nickname("payUser")
                .birthDate(LocalDate.of(1990, 1, 1))
                .profileImageUrl("https://example.com/image.jpg")
                .role(UserRole.USER)
                .build();

        testUser = userRepository.save(testUser);

        Provider provider = Provider.builder()
                .providerType(Provider.ProviderType.EMAIL)
                .providerId(testUser.getEmail())
                .user(testUser)
                .build();
        providerRepository.save(provider);

        accessToken = authTokenService.genAccessToken(testUser);
    }

    @Test
    @DisplayName("결제 생성 성공")
    void createPaymentSuccess() throws Exception {
        String requestBody = """
            {
                "amount": 10000
            }
            """;

        ResultActions result = mockMvc.perform(
                post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        ).andDo(print());

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("결제가 생성되었습니다."))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("결제 생성 실패 - 유효성 검증 실패(amount 누락)")
    void createPaymentFail_validation() throws Exception {
        String requestBody = """
            {
                "amount": null,
                "orderId": "ORDER-1234",
                "method": "CARD"
            }
            """;

        ResultActions result = mockMvc.perform(
                post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        ).andDo(print());

        result
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("결제 승인 성공")
    void confirmPaymentSuccess() throws Exception {
        // 결제 생성
        String createBody = """
            {
                "amount": 10000
            }
        """;

        MvcResult createResult = mockMvc.perform(
                post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        String orderId = JsonPath.read(createResponse, "$.data.orderId");

        TossConfirmResponseDto mockTossResponse = new TossConfirmResponseDto(
                "PAY-KEY-1234",
                orderId,
                "DONE",
                "카드",
                10000L,
                OffsetDateTime.now()
        );
        when(tossPaymentsClient.confirmPayment(any())).thenReturn(mockTossResponse);

        // 결제 승인
        String confirmBody = """
            {
                "paymentKey" : "PAY-KEY-1234",
                "orderId": "%s",
                "amount": 10000
            }
        """.formatted(orderId);

        ResultActions result = mockMvc.perform(
                post("/api/v1/payments/confirm")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody)
        ).andDo(print());

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("결제가 승인되었습니다."))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("결제 승인 실패 - 결제상태 검증(중복 승인)")
    void confirmPaymentFail_validation() throws Exception {

        // 결제 생성
        String createBody = """
            {
                "amount": 10000
            }
        """;

        MvcResult createResult = mockMvc.perform(
                post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        String orderId = JsonPath.read(createResponse, "$.data.orderId");

        TossConfirmResponseDto mockTossResponse = new TossConfirmResponseDto(
                "PAY-KEY-1234",
                orderId,
                "DONE",
                "카드",
                10000L,
                OffsetDateTime.now()
        );
        when(tossPaymentsClient.confirmPayment(any())).thenReturn(mockTossResponse);

        // 1차 승인 성공
        String confirmBody = """
        {
            "paymentKey" : "PAY-KEY-1234",
            "orderId": "%s",
            "amount": 10000
        }
        """.formatted(orderId);

        mockMvc.perform(
                        post("/api/v1/payments/confirm")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(confirmBody)
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("결제가 승인되었습니다."));

        // 2차 승인 실패
        mockMvc.perform(
                        post("/api/v1/payments/confirm")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(confirmBody)
                ).andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("P002"))
                .andExpect(jsonPath("$.msg").value("결제대기 상태가 아닙니다."));
    }

    @Test
    @DisplayName("결제 실패 처리 성공 - 사용자 결제 취소")
    void failPaymentSuccess() throws Exception {
        // 결제 생성
        String createBody = """
            {
                "amount": 10000
            }
        """;

        MvcResult createResult = mockMvc.perform(
                post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        String orderId = JsonPath.read(createResponse, "$.data.orderId");

        // 결제 실패 처리
        String requestBody = """
            {
                "orderId": "%s",
                "code": "PAY_PROCESS_CANCELED",
                "msg": "사용자에 의해 결제가 취소되었습니다."
            }
        """.formatted(orderId);

        ResultActions result = mockMvc.perform(
                post("/api/v1/payments/fail")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        ).andDo(print());

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("결제가 실패했습니다."))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("결제 실패 처리 실패 - 결제 상태 검증")
    void failPaymentFail_validation() throws Exception {
        // 결제 생성
        String createBody = """
            {
                "amount": 10000
            }
        """;

        MvcResult createResult = mockMvc.perform(
                post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        String orderId = JsonPath.read(createResponse, "$.data.orderId");

        // 결제 승인
        TossConfirmResponseDto mockTossResponse = new TossConfirmResponseDto(
                "PAY-KEY-1234",
                orderId,
                "DONE",
                "카드",
                10000L,
                OffsetDateTime.now()
        );
        when(tossPaymentsClient.confirmPayment(any())).thenReturn(mockTossResponse);

        String confirmBody = """
            {
                "paymentKey" : "PAY-KEY-1234",
                "orderId": "%s",
                "amount": 10000
            }
        """.formatted(orderId);

        mockMvc.perform(
                post("/api/v1/payments/confirm")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody)
        ).andDo(print())
                .andExpect(status().isOk());

        // 이미 승인된 결제에 대한 실패 처리 시도
        String requestBody = """
            {
                "orderId": "%s",
                "code": "PAY_PROCESS_CANCELED",
                "msg": "사용자에 의해 결제가 취소되었습니다."
            }
        """.formatted(orderId);

        ResultActions result = mockMvc.perform(
                post("/api/v1/payments/fail")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        ).andDo(print());

        result
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("P002"))
                .andExpect(jsonPath("$.msg").value("결제대기 상태가 아닙니다."));
    }

    @Test
    @DisplayName("결제 내역 조회 성공")
    void getPaymentHistorySuccess() throws Exception {
        // 결제 생성 및 승인
        String createBody = """
            {
                "amount": 10000
            }
        """;

        MvcResult createResult = mockMvc.perform(
                post("/api/v1/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        String orderId = JsonPath.read(createResponse, "$.data.orderId");

        OffsetDateTime approvedAt = OffsetDateTime.now();
        TossConfirmResponseDto mockTossResponse = new TossConfirmResponseDto(
                "PAY-KEY-1234",
                orderId,
                "DONE",
                "카드",
                10000L,
                approvedAt
        );
        when(tossPaymentsClient.confirmPayment(any())).thenReturn(mockTossResponse);

        String confirmBody = """
            {
                "paymentKey" : "PAY-KEY-1234",
                "orderId": "%s",
                "amount": 10000
            }
        """.formatted(orderId);

        mockMvc.perform(
                post("/api/v1/payments/confirm")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody)
        ).andDo(print())
                .andExpect(status().isOk());

        // 결제 내역 조회 (현재 시간을 포함하는 범위로 설정)
        OffsetDateTime startDate = approvedAt.minusDays(1);
        OffsetDateTime endDate = approvedAt.plusDays(1);

        String requestBody = String.format("""
            {
                "startDate" : "%s",
                "endDate": "%s",
                "status": "SUCCESS",
                "page": 0,
                "size": 1
            }
        """, startDate, endDate);

        ResultActions result = mockMvc.perform(
                get("/api/v1/payments/history")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        ).andDo(print());

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("결제 내역 조회에 성공했습니다."));
    }

    @Test
    @DisplayName("결제 내역 조회 실패 - 기간 검증 오류")
    void getPaymentHistoryFail_validation() throws Exception {
        String requestBody = """
            {
                "startDate" : "2025-12-31T23:59:59+09:00",
                "endDate": "2025-12-01T00:00:00+09:00",
                "status": "SUCCESS",
                "page": 0,
                "size": 1
            }
        """;

        ResultActions result = mockMvc.perform(
                get("/api/v1/payments/history")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        ).andDo(print());

        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("P005"))
                .andExpect(jsonPath("$.msg").value("종료일은 시작일보다 빠를 수 없습니다."));
    }
}