package devut.buzzerbidder.domain.chat.event;

import java.time.LocalDateTime;

public record DirectMessageSentEvent(
    Long chatMessageId,
    Long chatRoomId,
    Long senderId,
    String senderNickname,
    LocalDateTime sentAt
) {

}
