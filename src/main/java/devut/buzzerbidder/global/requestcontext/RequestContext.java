package devut.buzzerbidder.global.requestcontext;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.security.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestContext {
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Value("${cookie.domain:#{null}}")
    private String cookieDomain;
    
    @Value("${cookie.secure:true}")
    private boolean cookieSecure;
    
    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    public String getHeader(String name, String defaultValue) {
        return Optional
            .ofNullable(request.getHeader(name))
            .filter(headerValue -> !headerValue.isBlank())
            .orElse(defaultValue);
    }

    public String getCookieValue(String name, String defaultValue) {
        return Optional
            .ofNullable(request.getCookies())
            .flatMap(
                cookies ->
                    Arrays.stream(cookies)
                        .filter(cookie -> cookie.getName().equals(name))
                        .map(Cookie::getValue)
                        .filter(value -> !value.isBlank())
                        .findFirst()
            )
            .orElse(defaultValue);
    }

    public void setCookie(String name, String value) {
        if (value == null) value = "";

        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        
        // 도메인 설정: 환경 변수가 있으면 사용, 없으면 요청의 서버명 사용
        // localhost인 경우에는 도메인을 설정하지 않음 (브라우저가 자동으로 처리)
        String domain = cookieDomain != null && !cookieDomain.isBlank() 
            ? cookieDomain 
            : (request.getServerName() != null && !request.getServerName().equals("localhost") 
                ? request.getServerName() 
                : null);
        
        if (domain != null && !domain.isBlank()) {
            cookie.setDomain(domain);
        }
        
        // Secure 설정: 개발 환경에서는 false, 프로덕션에서는 true
        cookie.setSecure(cookieSecure);
        
        // SameSite 설정: OAuth2 리디렉션을 위해 Lax 또는 None 사용
        // Strict는 크로스 사이트 리디렉션에서 쿠키가 전달되지 않음
        cookie.setAttribute("SameSite", cookieSameSite);
        
        // 쿠키 만료 시간 설정 (기본값: 7일)
        if (!value.isBlank()) {
            cookie.setMaxAge(60 * 60 * 24 * 7); // 7일
        } else {
            // 값이 없다면 해당 쿠키변수를 삭제하라는 뜻
            cookie.setMaxAge(0);
        }

        response.addCookie(cookie);
    }

    public void deleteCookie(String name) {
        setCookie(name, null);
    }

    // 현재 인증된 사용자 정보 가져오기
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser();
    }

    public boolean isAuthenticated() {
        try {
            getCurrentUser();
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

}
