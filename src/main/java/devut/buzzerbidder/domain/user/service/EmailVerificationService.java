package devut.buzzerbidder.domain.user.service;

import java.security.SecureRandom;
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
        
        // 6자리 숫자 인증 코드 생성
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
     * 6자리 랜덤 숫자 코드 생성
     */
    private String generateRandomCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}

