package devut.buzzerbidder.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

@Schema(description = "회원 정보 수정 요청")
public record UserUpdateRequest(
        @Schema(description = "이메일", example = "new@user.com")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Schema(description = "닉네임", example = "gildong")
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/image.jpg")
        String image
) {
}

