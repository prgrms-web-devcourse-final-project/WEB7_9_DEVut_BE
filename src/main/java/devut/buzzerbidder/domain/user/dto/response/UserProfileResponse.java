package devut.buzzerbidder.domain.user.dto.response;

import devut.buzzerbidder.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "회원 프로필 정보")
public record UserProfileResponse(
        @Schema(description = "회원 ID", example = "1")
        Long id,

        @Schema(description = "이메일", example = "new@user.com")
        String email,

        @Schema(description = "닉네임", example = "gildong")
        String nickname,

        @Schema(description = "생년월일", example = "2000-01-01")
        LocalDate birth,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/image.jpg")
        String image,

        @Schema(description = "생성일", example = "2025-12-08")
        LocalDate createDate,

        @Schema(description = "수정일", example = "2025-12-09")
        LocalDate modifyDate
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getBirthDate(),
                user.getProfileImageUrl(),
                user.getCreateDate() != null ? user.getCreateDate().toLocalDate() : null,
                user.getModifyDate() != null ? user.getModifyDate().toLocalDate() : null
        );
    }
}

