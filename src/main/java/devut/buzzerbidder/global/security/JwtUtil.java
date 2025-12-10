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
        public static String toString(String secret, long expireSeconds, Map<String, Object> body) {
            ClaimsBuilder claimsBuilder = Jwts.claims();

            for (Map.Entry<String, Object> entry : body.entrySet()) {
                claimsBuilder.add(entry.getKey(), entry.getValue());
            }

            Claims claims = claimsBuilder.build();

            Date issuedAt = new Date();
            Date expiration = new Date(issuedAt.getTime() + 1000L * expireSeconds);

            Key secretKey = Keys.hmacShaKeyFor(secret.getBytes());

            return Jwts.builder()
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
        }

        public static boolean isValid(String jwt, String secretPattern) {

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
    }
}