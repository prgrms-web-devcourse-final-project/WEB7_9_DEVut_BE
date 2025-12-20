package devut.buzzerbidder.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record AuctionChatMessageResponse(
        @Schema(description = "메시지 임시 ID")
        String tempId,
        @Schema(description = "발신자 프로필 사진 URL")
        String profileImageUrl,
        @Schema(description = "발신자 닉네임")
        String nickname,
        @Schema(description = "메시지 내용")
        String message,
        @Schema(description = "메시지 발신 시간")
        LocalDateTime sendTime
) {
    public AuctionChatMessageResponse(String tempId, String profileImageUrl, String nickname, String message, LocalDateTime sendTime) {
        this.tempId = tempId;
        this.profileImageUrl = profileImageUrl;
        this.nickname = nickname;
        this.message = message;
        this.sendTime = sendTime;
    }
}
