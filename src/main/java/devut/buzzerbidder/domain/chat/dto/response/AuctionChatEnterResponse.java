package devut.buzzerbidder.domain.chat.dto.response;

import devut.buzzerbidder.domain.user.dto.response.UserInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "경매방 채팅 입장 응답")
public record AuctionChatEnterResponse(
        @Schema(description = "입장하려는 채팅방 ID")
        Long chatRoomId,

        @Schema(description = "현재 참여자 수")
        Long participantCount
) {}
