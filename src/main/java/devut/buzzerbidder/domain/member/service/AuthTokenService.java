package devut.buzzerbidder.domain.member.service;

import devut.buzzerbidder.domain.member.entity.Member;
import devut.buzzerbidder.global.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    @Value("${custom.jwt.secretPattern}")
    private String secretPattern;

    @Value("${custom.jwt.expireSeconds}")
    private long expireSeconds;

    @Value("${custom.jwt.refreshExpireSeconds:604800}") // 기본값 7일 (초)
    private long refreshExpireSeconds;

    public String genAccessToken(Member member) {
        return JwtUtil.jwt.toString(
                secretPattern,
                expireSeconds,
                Map.of("id", member.getId(), "email", member.getEmail(), "nickname",
                        member.getNickname())
        );
    }

    public String genRefreshToken(Member member) {
        return JwtUtil.jwt.toString(
                secretPattern,
                refreshExpireSeconds,
                Map.of("id", member.getId(), "email", member.getEmail(), "nickname",
                        member.getNickname())
        );
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
}
