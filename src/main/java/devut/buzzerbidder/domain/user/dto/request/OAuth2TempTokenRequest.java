package devut.buzzerbidder.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OAuth2TempTokenRequest(
    @NotBlank(message = "임시 토큰은 필수입니다.")
    String tempToken
) {
}

