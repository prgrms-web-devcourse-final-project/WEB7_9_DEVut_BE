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
                .birthDate(LocalDate.of(1990, 1, 1))
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

}
