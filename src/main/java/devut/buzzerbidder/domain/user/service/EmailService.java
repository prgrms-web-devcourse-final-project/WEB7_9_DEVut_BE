package devut.buzzerbidder.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${custom.email.verification.subject:이메일 인증 코드}")
    private String subject;

    /**
     * 이메일 인증 코드 발송
     * @param to 수신자 이메일
     * @param code 인증 코드
     */
    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(buildEmailContent(code));
        
        mailSender.send(message);
    }

    /**
     * 이메일 본문 생성
     */
    private String buildEmailContent(String code) {
        return """
            안녕하세요. BuzzerBidder입니다.
            
            회원가입을 위한 이메일 인증 코드입니다.
            
            인증 코드: %s
            
            이 코드는 10분간 유효합니다.
            본인이 요청하지 않은 경우 이 이메일을 무시하셔도 됩니다.
            
            감사합니다.
            """.formatted(code);
    }
}

