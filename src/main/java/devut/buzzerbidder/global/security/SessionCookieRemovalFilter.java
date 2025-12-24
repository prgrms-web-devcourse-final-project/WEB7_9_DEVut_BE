package devut.buzzerbidder.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JSESSIONID 쿠키를 제거하여 stateless 환경을 유지하는 필터
 * OAuth2 로그인 플로우에서 생성될 수 있는 세션 쿠키를 제거합니다.
 */
@Slf4j
@Component
@Order(1)
public class SessionCookieRemovalFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // 필터 체인 실행
        filterChain.doFilter(request, response);
        
        // 모든 응답에서 JSESSIONID 쿠키를 명시적으로 제거 (stateless 유지)
        // OAuth2 로그인 플로우에서 생성될 수 있는 세션 쿠키를 방지
        Cookie jsessionCookie = new Cookie("JSESSIONID", null);
        jsessionCookie.setPath("/");
        jsessionCookie.setMaxAge(0);
        jsessionCookie.setHttpOnly(true);
        // 도메인 설정 (필요시 환경 변수로 설정 가능)
        if (request.getServerName() != null) {
            jsessionCookie.setDomain(request.getServerName());
        }
        response.addCookie(jsessionCookie);
        
        log.debug("JSESSIONID 쿠키 제거 완료: {}", request.getRequestURI());
    }
}

