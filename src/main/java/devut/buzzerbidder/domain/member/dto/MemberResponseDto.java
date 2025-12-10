package devut.buzzerbidder.domain.member.dto;

import devut.buzzerbidder.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public class MemberResponseDto {

    @Schema(description = "로그인 응답")
    public record LoginResponse(
            @Schema(description = "회원 정보")
            MemberInfo memberInfo
    ) {
        public static LoginResponse of(Member member) {
            return new LoginResponse(
                    MemberInfo.from(member)
            );
        }
    }

    @Schema(description = "회원 정보")
    public record MemberInfo(
            @Schema(description = "회원 ID", example = "1")
            Long id,

            @Schema(description = "이메일", example = "user@example.com")
            String email,

            @Schema(description = "이름", example = "홍길동")
            String name,

            @Schema(description = "닉네임", example = "hong123")
            String nickname,

            @Schema(description = "생년월일", example = "1990-01-01")
            LocalDate birthDate,

            @Schema(description = "프로필 이미지 URL", example = "https://example.com/image.jpg")
            String profileImageUrl
    ) {
        public static MemberInfo from(Member member) {
            return new MemberInfo(
                    member.getId(),
                    member.getEmail(),
                    member.getName(),
                    member.getNickname(),
                    member.getBirthDate(),
                    member.getProfileImageUrl()
            );
        }
    }
}

