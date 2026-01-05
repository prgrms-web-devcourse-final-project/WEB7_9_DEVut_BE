package devut.buzzerbidder.domain.notification.listener;

import devut.buzzerbidder.domain.chat.entity.ChatMessage;
import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import devut.buzzerbidder.domain.chat.event.DirectMessageSentEvent;
import devut.buzzerbidder.domain.chat.repository.ChatMessageRepository;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.service.NotificationService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DirectMessageSentNotificationListener {

    private final NotificationService notificationService;
    private final ChatMessageRepository chatMessageRepository;
    private final DelayedItemRepository delayedItemRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFirstSent(DirectMessageSentEvent event) {

        // chatMessageId로 메시지와 채팅방 한 번에 조회
        ChatMessage chatMessage = chatMessageRepository.findByIdWithChatRoom(event.chatMessageId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));

        ChatRoom chatRoom = chatMessage.getChatRoom();

        // DM 채팅방이고, 경매품 참조인지 먼저 확인
        if (chatRoom.getRoomType() != ChatRoom.RoomType.DM ||
            chatRoom.getReferenceType() != ChatRoom.ReferenceEntityType.ITEM) {
            return;
        }

        // 첫 메시지인지 확인
        long messageCount = chatMessageRepository.countByChatRoom(chatRoom);
        if (messageCount != 1) {
            return;
        }

        // 지연 경매 상품 조회
        Long itemId = chatRoom.getReferenceEntityId();
        DelayedItem delayedItem = delayedItemRepository.findById(itemId)
            .orElse(null);

        if (delayedItem == null) {
            return;
        }

        String message = "%s님이 '%s' 상품에 대해 메시지를 보냈습니다."
            .formatted(event.senderNickname(), delayedItem.getName());

        notificationService.createAndSend(
            delayedItem.getSellerUserId(),
            NotificationType.DM_FIRST_MESSAGE,
            message,
            "DELAYED_ITEM",
            itemId,
            Map.of(
                "itemName", delayedItem.getName(),
                "chatRoomId", event.chatRoomId(),
                "senderNickname", event.senderNickname(),
                "createDate", event.sentAt()
            )
        );
    }
}
