package devut.buzzerbidder.domain.user.dto.response;

import devut.buzzerbidder.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 정보 조회 응답")
public record UserInfoResponse(
        @Schema(description = "회원 정보")
        UserInfo userInfo,

        @Schema(description = "회원의 Bizz 잔액", example = "38200")
        Long bizz
) {
    public static UserInfoResponse of(User user, Long bizz) {
        return new UserInfoResponse(
                UserInfo.from(user),
                bizz
        );
    }
}
