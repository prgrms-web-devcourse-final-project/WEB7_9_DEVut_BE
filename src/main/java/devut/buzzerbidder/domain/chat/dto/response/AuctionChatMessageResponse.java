package devut.buzzerbidder.domain.chat.dto.response;

import java.time.LocalDateTime;

public record AuctionChatMessageResponse(
        String tempId,
        String nickname,
        String message,
        LocalDateTime sendTime
) {
    public AuctionChatMessageResponse(String tempId, String nickname, String message, LocalDateTime sendTime) {
        this.tempId = tempId;
        this.nickname = nickname;
        this.message = message;
        this.sendTime = sendTime;
    }
}
