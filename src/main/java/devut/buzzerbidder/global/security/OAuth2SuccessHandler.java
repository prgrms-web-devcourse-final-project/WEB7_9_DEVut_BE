package devut.buzzerbidder.global.security;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
            // 실패해도 프론트엔드로 리다이렉트
            getRedirectStrategy().sendRedirect(request, response, frontendBaseUrl + "/oauth2/success");
            return;
        }

        log.info("OAuth2 사용자 인증 완료: userId={}, email={}", user.getId(), user.getEmail());

        // JWT 토큰 생성
        String accessToken = authTokenService.genAccessToken(user);
        String refreshToken = authTokenService.genRefreshToken(user);

        log.debug("JWT 토큰 생성 완료");

        // 쿠키에 토큰 설정 (리다이렉트 후에도 유지됨)
        log.info("쿠키 설정 시작: accessToken 길이={}, refreshToken 길이={}",
                accessToken != null ? accessToken.length() : 0,
                refreshToken != null ? refreshToken.length() : 0);
        requestContext.setCookie("accessToken", accessToken);
        requestContext.setCookie("refreshToken", refreshToken);
        log.info("쿠키 설정 완료: requestURI={}, serverName={}",
                request.getRequestURI(), request.getServerName());

        SecurityContextHolder.clearContext();
        
        // 세션 무효화 (stateless 유지)
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.debug("OAuth2 로그인 중 생성된 세션 무효화");
            session.invalidate();
        }

        // 프론트엔드로 리다이렉트 (쿠키에 토큰이 포함되어 있음)
        String redirectUrl = frontendBaseUrl + "/oauth2/success";
        log.info("OAuth2 로그인 성공, 프론트엔드로 리다이렉트: userId={}, redirectUrl={}", user.getId(), redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

