package devut.buzzerbidder.domain.chat.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ChatListResponse(
        List<ChatRoomItem> chatRooms
) {
    public record ChatRoomItem(
            Long chatRoomId,
            String otherUserNickname,
            String otherUserProfileImage,
            String lastMessage,
            LocalDateTime lastMessageTime,
            boolean hasUnreadMessage
    ) {}
}
