package devut.buzzerbidder.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "SNS 로그인 요청")
public record SnsLoginRequest(
        @Schema(description = "SNS 제공자 ID", example = "123456789123456789")
        @NotBlank(message = "Provider ID는 필수입니다.")
        String providerId
) {
}

