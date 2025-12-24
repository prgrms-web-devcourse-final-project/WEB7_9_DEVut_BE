package devut.buzzerbidder.global.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

/**
 * 쿠키 관련 유틸리티 클래스
 * OAuth2 인증 요청 정보를 쿠키에 저장/로드하기 위한 헬퍼 메서드 제공
 */
@Slf4j
public class CookieUtils {

    /**
     * 쿠키에서 값을 가져옵니다.
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 쿠키를 추가합니다.
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    /**
     * 쿠키를 삭제합니다.
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    cookie.setHttpOnly(true);
                    cookie.setSecure(true);
                    response.addCookie(cookie);
                }
            }
        }
    }

    /**
     * 객체를 Base64로 직렬화합니다.
     * OAuth2AuthorizationRequest와 같은 복잡한 객체를 쿠키에 저장하기 위해 사용합니다.
     */
    public static String serialize(Object object) {
        try {
            byte[] bytes = SerializationUtils.serialize(object);
            return Base64.getUrlEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.error("객체 직렬화 실패", e);
            throw new RuntimeException("객체 직렬화 실패", e);
        }
    }

    /**
     * Base64 문자열을 객체로 역직렬화합니다.
     */
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cookie.getValue());
            return cls.cast(SerializationUtils.deserialize(bytes));
        } catch (Exception e) {
            log.error("객체 역직렬화 실패: cookieName={}", cookie.getName(), e);
            return null;
        }
    }
}

