package devut.buzzerbidder.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ClaimsBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtUtil {
    public static class jwt {
        private static final int MIN_SECRET_KEY_LENGTH = 32; // 256 bits (32 bytes)
        public static String toString(String secret, long expireSeconds, Map<String, Object> body) {
            validateSecretKey(secret);
            
            ClaimsBuilder claimsBuilder = Jwts.claims();

            for (Map.Entry<String, Object> entry : body.entrySet()) {
                claimsBuilder.add(entry.getKey(), entry.getValue());
            }

            Claims claims = claimsBuilder.build();

            Date issuedAt = new Date();
            Date expiration = new Date(issuedAt.getTime() + 1000L * expireSeconds);

            Key secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            return Jwts.builder()
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
        }

        public static boolean isValid(String jwt, String secretPattern) {
            validateSecretKey(secretPattern);

            SecretKey secretKey = Keys.hmacShaKeyFor(secretPattern.getBytes(StandardCharsets.UTF_8));

            try {
                Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(jwt);

            } catch (Exception e) {
                return false;
            }

            return true;
        }

        public static Map<String, Object> payloadOrNull(String jwt, String secretPattern) {
            validateSecretKey(secretPattern);

            SecretKey secretKey = Keys.hmacShaKeyFor(secretPattern.getBytes(StandardCharsets.UTF_8));

            if(isValid(jwt, secretPattern)) {
                Claims claims = (Claims) Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(jwt)
                    .getPayload();
                return new HashMap<>(claims);
            }

            return null;
        }
        
        /**
         * JWT Secret Key의 최소 길이를 검증합니다.
         * HMAC-SHA 알고리즘을 사용하려면 최소 256비트(32바이트)가 필요합니다.
         * @param secret 검증할 secret key
         * @throws IllegalArgumentException secret key가 너무 짧은 경우
         */
        private static void validateSecretKey(String secret) {
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_KEY_LENGTH) {
                throw new IllegalArgumentException(
                    String.format("JWT secret key must be at least %d bytes (256 bits) long. " +
                        "Current length: %d bytes. " +
                        "Please set a longer secret key in your configuration.",
                        MIN_SECRET_KEY_LENGTH,
                        secret != null ? secret.getBytes(StandardCharsets.UTF_8).length : 0)
                );
            }
        }
    }
}