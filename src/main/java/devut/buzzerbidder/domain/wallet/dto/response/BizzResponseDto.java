package devut.buzzerbidder.domain.wallet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record BizzResponseDto(
        @Schema(description = "bizz 잔액")
        Long bizz
) {
    public static BizzResponseDto from(Long bizz) {
        return new BizzResponseDto(bizz);
    }
}
