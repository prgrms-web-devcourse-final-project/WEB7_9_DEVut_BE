package devut.buzzerbidder.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "SNS 회원가입 요청")
public record SnsSignUpRequest(
        @Schema(description = "이메일", example = "new@user.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Schema(description = "SNS 제공자 ID", example = "123456789123456789")
        @NotBlank(message = "Provider ID는 필수입니다.")
        String providerId,

        @Schema(description = "닉네임", example = "gildong")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다.")
        String nickname,

        @Schema(description = "생년월일", example = "2000-01-01")
        @NotNull(message = "생년월일은 필수입니다.")
        LocalDate birth,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/image.jpg")
        String image
) {
}


