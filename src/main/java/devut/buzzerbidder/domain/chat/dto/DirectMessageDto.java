package devut.buzzerbidder.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record DirectMessageDto(
        @Schema(description = "메시지 ID")
        Long id,

        @Schema(description = "발신자 ID")
        Long senderId,

        @Schema(description = "발신자 프로필 사진 URL")
        String profileImageUrl,

        @Schema(description = "발신자 닉네임")
        String nickname,

        @Schema(description = "메시지 내용")
        String content,

        @Schema(description = "메시지 발신 시간")
        LocalDateTime sendTime
) {}