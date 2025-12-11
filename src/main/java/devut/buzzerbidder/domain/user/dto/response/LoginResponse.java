package devut.buzzerbidder.domain.user.dto.response;

import devut.buzzerbidder.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "회원 정보")
        UserInfo userInfo
) {
    public static LoginResponse of(User user) {
        return new LoginResponse(
                UserInfo.from(user)
        );
    }
}

