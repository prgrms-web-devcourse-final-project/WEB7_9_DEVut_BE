package devut.buzzerbidder.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record DirectMessageResponse(
        @Schema(description = "메시지 타입", example = "DM")
        String type,

        @Schema(description = "메시지 ID")
        Long id,

        @Schema(description = "발신자 프로필 사진 URL")
        String profileImageUrl,

        @Schema(description = "발신자 닉네임")
        String nickname,

        @Schema(description = "메시지 내용")
        String content,

        @Schema(description = "메시지 발신 시간")
        LocalDateTime sendTime
) {}
