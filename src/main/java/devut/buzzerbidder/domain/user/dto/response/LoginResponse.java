package devut.buzzerbidder.domain.user.dto.response;

import devut.buzzerbidder.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "회원 정보")
        UserInfo userInfo,
        @Schema(description = "Access Token")
        String accessToken,
        @Schema(description = "Refresh Token")
        String refreshToken
) {
    public static LoginResponse of(User user) {
        return new LoginResponse(
                UserInfo.from(user),
                null,
                null
        );
    }

    public static LoginResponse of(User user, String accessToken, String refreshToken) {
        return new LoginResponse(
                UserInfo.from(user),
                accessToken,
                refreshToken
        );
    }

    public static LoginResponse of(User user, String accessToken, String refreshToken) {
        return new LoginResponse(
            UserInfo.from(user),
            accessToken,
            refreshToken
        );
    }
}