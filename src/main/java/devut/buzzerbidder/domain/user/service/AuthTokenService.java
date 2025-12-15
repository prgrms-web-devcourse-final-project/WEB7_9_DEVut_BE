package devut.buzzerbidder.domain.user.service;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @Value("${custom.jwt.secretPattern}")
    private String secretPattern;

    @Value("${custom.jwt.expireSeconds}")
    private long expireSeconds;

    @Value("${custom.jwt.refreshExpireSeconds:604800}") // 기본값 7일 (초)
    private long refreshExpireSeconds;

    public String genAccessToken(User user) {
        return JwtUtil.jwt.toString(
                secretPattern,
                expireSeconds,
                Map.of("id", user.getId(), "email", user.getEmail(), "nickname",
                        user.getNickname())
        );
    }

    public String genRefreshToken(User user) {
        String refreshToken = JwtUtil.jwt.toString(
                secretPattern,
                refreshExpireSeconds,
                Map.of("id", user.getId(), "email", user.getEmail(), "nickname",
                        user.getNickname())
        );

        // Redis에 refresh token 저장
        refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

        return refreshToken;
    }

    public Map<String, Object> payloadOrNull(String jwt) {
        Map<String, Object> payload = JwtUtil.jwt.payloadOrNull(jwt, secretPattern);

        if (payload == null) {
            return null;
        }

        Number idNo = (Number) payload.get("id");
        long id = idNo.longValue();
        String email = (String) payload.get("email");
        String nickname = (String) payload.get("nickname");

        return Map.of("id", id, "email", email, "nickname", nickname);
    }

    /**
     * Refresh Token을 검증하고 해당하는 User를 반환합니다.
     *
     * @param refreshToken 검증할 refresh token
     * @return 검증된 User 엔티티
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    public User validateAndGetUserByRefreshToken(String refreshToken) {
        // 1. Refresh Token 검증
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.USER_TOKEN_INVALID);
        }

        // 2. JWT에서 payload 추출
        Map<String, Object> payload = payloadOrNull(refreshToken);
        if (payload == null) {
            throw new BusinessException(ErrorCode.USER_TOKEN_INVALID);
        }

        // 3. User ID 추출
        Number idNo = (Number) payload.get("id");
        Long userId = idNo.longValue();

        // 4. Redis에서 저장된 refresh token과 비교
        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            throw new BusinessException(ErrorCode.USER_TOKEN_INVALID);
        }

        // 5. User 조회 및 반환
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}