package devut.buzzerbidder.global.security;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenService authTokenService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        logger.debug("CustomAuthenticationFilter called");

        try {
            authenticate(request, response, filterChain);
        } catch (BusinessException e) {
            ErrorCode errorCode = e.getErrorCode();
            response.setContentType("application/json; charset=UTF-8");
            response.setStatus(errorCode.getStatus().value());
            response.getWriter().write("""
                    {
                        "resultCode": "%s",
                        "msg": "%s"
                    }
                    """.formatted(errorCode.getCode(), errorCode.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error in authentication filter", e);
            throw e;
        }
    }

    private void authenticate(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // API 경로가 아닌 경우 통과 (Swagger UI, H2 Console, WebSocket 등)
        if(!requestURI.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 허용된 API 경로들 (회원가입, 로그인)
        if(requestURI.equals("/api/v1/users/signup") || 
           requestURI.equals("/api/v1/users/signin")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken;
        String headerAuthorization = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuthorization) && headerAuthorization.startsWith("Bearer ")) {
            accessToken = headerAuthorization.substring(7);
        } else {
            // 쿠키에서 토큰 찾기
            accessToken = getCookieValue(request, "accessToken");
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        Map<String, Object> payload = authTokenService.payloadOrNull(accessToken);

        if (payload == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        long id = ((Number) payload.get("id")).longValue();

        // 데이터베이스에서 실제 User 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserDetails userDetails = new CustomUserDetails(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            userDetails.getPassword(),
            userDetails.getAuthorities()
        );

        SecurityContextHolder
            .getContext()
            .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals(name))
                    .map(Cookie::getValue)
                    .filter(value -> !value.isBlank())
                    .findFirst()
                    .orElse("");
        }
        return "";
    }
}