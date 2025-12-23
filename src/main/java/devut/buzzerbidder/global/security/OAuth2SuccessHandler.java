package devut.buzzerbidder.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import devut.buzzerbidder.domain.user.dto.response.LoginResponse;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import devut.buzzerbidder.global.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthTokenService authTokenService;
    private final RequestContext requestContext;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 인증 성공: requestURI={}", request.getRequestURI());
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        User user;
        
        // CustomOAuth2User인 경우 getUser() 메서드 사용
        if (oAuth2User instanceof CustomOAuth2User) {
            user = ((CustomOAuth2User) oAuth2User).getUser();
        } else {
            // 일반 OAuth2User인 경우 attributes에서 가져오기
            user = (User) oAuth2User.getAttributes().get("user");
        }
        
        if (user == null) {
            log.error("OAuth2 인증 성공했으나 User 정보를 찾을 수 없습니다.");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "인증 처리 중 오류가 발생했습니다.");
            return;
        }

        log.info("OAuth2 사용자 인증 완료: userId={}, email={}", user.getId(), user.getEmail());

        // JWT 토큰 생성
        String accessToken = authTokenService.genAccessToken(user);
        String refreshToken = authTokenService.genRefreshToken(user);

        log.debug("JWT 토큰 생성 완료");

        // 헤더에 토큰 설정
        requestContext.setHeader("Authorization", "Bearer " + accessToken);
        requestContext.setHeader("Refresh-Token", refreshToken);

        // 쿠키에 토큰 설정
        requestContext.setCookie("accessToken", accessToken);
        requestContext.setCookie("refreshToken", refreshToken);

        // LoginResponse 생성 (로그인 API와 동일한 형식)
        LoginResponse loginResponse = LoginResponse.of(user, accessToken, refreshToken);
        ApiResponse<LoginResponse> apiResponse = ApiResponse.ok("소셜 로그인에 성공했습니다.", loginResponse);

        // JSON 응답 전송
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();

        log.info("OAuth2 로그인 성공, JSON 응답 전송: userId={}", user.getId());
    }
}

