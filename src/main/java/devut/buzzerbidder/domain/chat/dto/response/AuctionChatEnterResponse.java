package devut.buzzerbidder.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "경매방 채팅 입장 응답")
public record AuctionChatEnterResponse(
        @Schema(description = "입장하려는 채팅방 ID")
        Long chatRoomId
) {
    public AuctionChatEnterResponse(Long chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
}
