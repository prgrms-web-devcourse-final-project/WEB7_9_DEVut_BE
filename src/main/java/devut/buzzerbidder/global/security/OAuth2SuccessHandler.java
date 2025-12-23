package devut.buzzerbidder.global.security;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthTokenService authTokenService;
    private final RequestContext requestContext;

    @Value("${frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

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

        // state 파라미터에서 리다이렉트 URL 추출
        String state = request.getParameter("state");
        String redirectUrl = frontendBaseUrl + "/"; // 기본 리다이렉트 URL (프로파일별 설정 사용)

        if (state != null && !state.isBlank()) {
            try {
                String decodedState = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
                // state 형식: "prefix#redirectUrl" 또는 "redirectUrl"
                if (decodedState.contains("#")) {
                    redirectUrl = decodedState.split("#")[1];
                } else {
                    redirectUrl = decodedState;
                }
                log.debug("State에서 리다이렉트 URL 추출: {}", redirectUrl);
            } catch (Exception e) {
                log.warn("State 파라미터 디코딩 실패, 기본 URL 사용: {}", e.getMessage());
            }
        }

        String separator = redirectUrl.contains("?") ? "&" : "?";
        String finalRedirectUrl = redirectUrl + separator + "oauth2=success&userId=" + user.getId();

        log.info("OAuth2 로그인 성공, 프론트엔드로 리다이렉트: userId={}, redirectUrl={}", user.getId(), finalRedirectUrl);
        getRedirectStrategy().sendRedirect(request, response, finalRedirectUrl);
    }
}

