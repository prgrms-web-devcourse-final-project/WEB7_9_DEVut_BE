package devut.buzzerbidder.domain.user.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${custom.email.verification.expireMinutes:10}")
    private long expireMinutes;

    private static final String VERIFICATION_CODE_PREFIX = "email_verification:";
    private static final String VERIFIED_EMAIL_PREFIX = "verified_email:";
    private static final int CODE_LENGTH = 6;

    /**
     * 이메일 인증 코드 생성 및 저장
     * @param email 이메일 주소
     * @return 생성된 인증 코드
     */
    public String generateAndSaveVerificationCode(String email) {
        // 기존 인증 코드가 있으면 삭제 (재발송 시)
        String existingCodeKey = VERIFICATION_CODE_PREFIX + email;
        redisTemplate.delete(existingCodeKey);
        
        // 기존 인증 완료 상태도 삭제 (재인증 유도)
        String existingVerifiedKey = VERIFIED_EMAIL_PREFIX + email;
        redisTemplate.delete(existingVerifiedKey);
        
        // 6자리 인증 코드 생성 (숫자, 알파벳, 특수문자 포함)
        String code = generateRandomCode();
        
        // Redis에 저장 (만료 시간: 10분)
        String key = VERIFICATION_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, expireMinutes, TimeUnit.MINUTES);
        
        return code;
    }

    /**
     * 이메일 인증 코드 검증
     * @param email 이메일 주소
     * @param code 인증 코드
     * @return 검증 성공 여부
     */
    public boolean verifyCode(String email, String code) {
        String key = VERIFICATION_CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        
        if (storedCode == null || !storedCode.equals(code)) {
            return false;
        }
        
        // 인증 성공 시 인증 코드 삭제 및 인증 완료 표시
        redisTemplate.delete(key);
        
        // 인증 완료된 이메일 저장 (회원가입 시 사용, 30분 유효)
        String verifiedKey = VERIFIED_EMAIL_PREFIX + email;
        redisTemplate.opsForValue().set(verifiedKey, "verified", 30, TimeUnit.MINUTES);
        
        return true;
    }

    /**
     * 이메일 인증 완료 여부 확인
     * @param email 이메일 주소
     * @return 인증 완료 여부
     */
    public boolean isEmailVerified(String email) {
        String key = VERIFIED_EMAIL_PREFIX + email;
        String verified = redisTemplate.opsForValue().get(key);
        return verified != null && verified.equals("verified");
    }

    /**
     * 인증 완료된 이메일 삭제 (회원가입 완료 후)
     * @param email 이메일 주소
     */
    public void deleteVerifiedEmail(String email) {
        String key = VERIFIED_EMAIL_PREFIX + email;
        redisTemplate.delete(key);
    }

    /**
     * 이메일 인증 코드의 남은 시간 조회 (초 단위)
     * @param email 이메일 주소
     * @return 남은 시간(초), 인증 코드가 없으면 0
     */
    public Long getRemainingSeconds(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0L;
    }

    /**
     * 이메일 인증 코드의 만료 시간 계산
     * @param email 이메일 주소
     * @return 만료 시간(LocalDateTime), 인증 코드가 없으면 null
     */
    public LocalDateTime getExpiresAt(String email) {
        Long remainingSeconds = getRemainingSeconds(email);
        if (remainingSeconds == null || remainingSeconds <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(
            Instant.now().plusSeconds(remainingSeconds),
            ZoneId.systemDefault()
        );
    }

    /**
     * 6자리 랜덤 인증 코드 생성 (숫자, 알파벳 대소문자, 특수문자 포함)
     */
    private String generateRandomCode() {
        SecureRandom random = new SecureRandom();
        // 사용 가능한 문자 집합: 숫자(0-9), 대문자(A-Z), 소문자(a-z), 특수문자(!@#$%^&*)
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }
}

