package devut.buzzerbidder.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "이메일 인증 코드 발송 응답")
public record EmailVerificationResponse(
        @Schema(description = "남은 시간(초)", example = "600")
        Long remainingSeconds,
        
        @Schema(description = "만료 시간", example = "2025-01-15T20:00:00")
        LocalDateTime expiresAt
) {
}
