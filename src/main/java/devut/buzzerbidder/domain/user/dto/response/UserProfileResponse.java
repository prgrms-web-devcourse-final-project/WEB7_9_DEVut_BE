package devut.buzzerbidder.domain.user.dto.response;

import devut.buzzerbidder.domain.user.entity.DeliveryAddress;
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

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/image.jpg")
        String image,

        @Schema(description = "주소", example = "서울시 중구 세종대로 135-5")
        String address,

        @Schema(description = "상세주소", example = "OO아파트 101동 101호")
        String addressDetail,

        @Schema(description = "우편번호", example = "12345")
        String postalCode,

        @Schema(description = "생성일", example = "2025-12-08")
        LocalDate createDate,

        @Schema(description = "수정일", example = "2025-12-09")
        LocalDate modifyDate,

        @Schema(description = "보유 Bizz 잔액", example = "38200")
        Long bizz
) {
    public static UserProfileResponse from(User user, Long bizz, DeliveryAddress deliveryAddress) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                deliveryAddress.getAddress(),
                deliveryAddress.getAddressDetail(),
                deliveryAddress.getPostalCode(),
                user.getCreateDate() != null ? user.getCreateDate().toLocalDate() : null,
                user.getModifyDate() != null ? user.getModifyDate().toLocalDate() : null,
                bizz
        );
    }
}

