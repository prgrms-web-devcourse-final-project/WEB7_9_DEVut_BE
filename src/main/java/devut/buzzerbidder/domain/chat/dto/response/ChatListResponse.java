package devut.buzzerbidder.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "DM 채팅방 목록 응답")
public record ChatListResponse(
        @Schema(description = "채팅방 목록")
        List<ChatRoomItem> chatRooms
) {
    @Schema(description = "채팅방 아이템")
    public record ChatRoomItem(
            @Schema(description = "채팅방 ID")
            Long chatRoomId,

            @Schema(description = "상품 ID")
            Long itemId,

            @Schema(description = "상대방 닉네임")
            String otherUserNickname,

            @Schema(description = "상대방 프로필 이미지 URL")
            String otherUserProfileImage,

            @Schema(description = "마지막 메시지 내용")
            String lastMessage,

            @Schema(description = "마지막 메시지 시간")
            LocalDateTime lastMessageTime,

            @Schema(description = "읽지 않은 메시지 존재 여부")
            boolean hasUnreadMessage
    ) {}
}
