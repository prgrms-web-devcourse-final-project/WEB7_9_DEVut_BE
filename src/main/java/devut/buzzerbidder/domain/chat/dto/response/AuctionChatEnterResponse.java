package devut.buzzerbidder.domain.chat.dto.response;

import devut.buzzerbidder.domain.user.dto.response.UserInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "경매방 채팅 입장 응답")
public record AuctionChatEnterResponse(
        @Schema(description = "입장하려는 채팅방 ID")
        Long chatRoomId,
        
        @Schema(description = "입장한 유저들의 프로필 정보")
        List<UserInfo> users
) {
    public AuctionChatEnterResponse(Long chatRoomId, List<UserInfo> users) {
        this.chatRoomId = chatRoomId;
        this.users = users;
    }
}
