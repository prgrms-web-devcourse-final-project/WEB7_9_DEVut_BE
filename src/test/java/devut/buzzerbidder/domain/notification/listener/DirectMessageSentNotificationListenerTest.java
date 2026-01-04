package devut.buzzerbidder.domain.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import devut.buzzerbidder.domain.chat.entity.ChatMessage;
import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import devut.buzzerbidder.domain.chat.event.DirectMessageSentEvent;
import devut.buzzerbidder.domain.chat.repository.ChatMessageRepository;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DirectMessageSentNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private DelayedItemRepository delayedItemRepository;

    @InjectMocks
    private DirectMessageSentNotificationListener listener;

    // ========== 첫 메시지 알림 전송 테스트 ==========

    @Test
    @DisplayName("t1: 구매자가 첫 DM 메시지 전송시 판매자에게 알림 전송")
    void t1() {
        // given
        Long chatMessageId = 1L;
        Long chatRoomId = 100L;
        Long buyerId = 2L;
        String buyerNickname = "구매자";
        Long sellerId = 1L;
        Long itemId = 999L;

        ChatRoom chatRoom = mock(ChatRoom.class);
        when(chatRoom.getRoomType()).thenReturn(ChatRoom.RoomType.DM);
        when(chatRoom.getReferenceType()).thenReturn(ChatRoom.ReferenceEntityType.ITEM);
        when(chatRoom.getReferenceEntityId()).thenReturn(itemId);

        ChatMessage chatMessage = mock(ChatMessage.class);
        when(chatMessage.getChatRoom()).thenReturn(chatRoom);

        DelayedItem delayedItem = DelayedItem.builder()
            .name("테스트 상품")
            .sellerUserId(sellerId)
            .build();

        DirectMessageSentEvent event = new DirectMessageSentEvent(
            chatMessageId,
            chatRoomId,
            buyerId,
            buyerNickname,
            LocalDateTime.now()
        );

        when(chatMessageRepository.findByIdWithChatRoom(chatMessageId))
            .thenReturn(Optional.of(chatMessage));
        when(chatMessageRepository.countByChatRoom(chatRoom))
            .thenReturn(1L);
        when(delayedItemRepository.findById(itemId))
            .thenReturn(Optional.of(delayedItem));

        // when
        listener.handleFirstSent(event);

        // then
        verify(notificationService).createAndSend(
            eq(sellerId),
            eq(NotificationType.DM_FIRST_MESSAGE),
            contains("메시지를 보냈습니다"),
            eq("DELAYED_ITEM"),
            eq(itemId),
            anyMap()
        );
    }

    @Test
    @DisplayName("t2: 두 번째 메시지인 경우 알림 전송하지 않음")
    void t2() {
        // given
        Long chatMessageId = 2L;
        Long buyerId = 2L;

        ChatRoom chatRoom = mock(ChatRoom.class);
        when(chatRoom.getRoomType()).thenReturn(ChatRoom.RoomType.DM);
        when(chatRoom.getReferenceType()).thenReturn(ChatRoom.ReferenceEntityType.ITEM);

        ChatMessage chatMessage = mock(ChatMessage.class);
        when(chatMessage.getChatRoom()).thenReturn(chatRoom);

        DirectMessageSentEvent event = new DirectMessageSentEvent(
            chatMessageId, 100L, buyerId, "구매자", LocalDateTime.now()
        );

        when(chatMessageRepository.findByIdWithChatRoom(chatMessageId))
            .thenReturn(Optional.of(chatMessage));
        when(chatMessageRepository.countByChatRoom(chatRoom))
            .thenReturn(2L); // 두 번째 메시지

        // when
        listener.handleFirstSent(event);

        // then
        verify(notificationService, never()).createAndSend(
            anyLong(), any(), any(), any(), anyLong(), anyMap()
        );
    }

    @Test
    @DisplayName("t3: 판매자가 첫 메시지를 보낸 경우 알림 전송하지 않음")
    void t3() {
        // given
        Long chatMessageId = 1L;
        Long sellerId = 1L;
        Long itemId = 999L;

        ChatRoom chatRoom = mock(ChatRoom.class);
        when(chatRoom.getRoomType()).thenReturn(ChatRoom.RoomType.DM);
        when(chatRoom.getReferenceType()).thenReturn(ChatRoom.ReferenceEntityType.ITEM);
        when(chatRoom.getReferenceEntityId()).thenReturn(itemId);

        ChatMessage chatMessage = mock(ChatMessage.class);
        when(chatMessage.getChatRoom()).thenReturn(chatRoom);

        DelayedItem delayedItem = DelayedItem.builder()
            .name("테스트 상품")
            .sellerUserId(sellerId)
            .build();

        DirectMessageSentEvent event = new DirectMessageSentEvent(
            chatMessageId, 100L, sellerId, "판매자", LocalDateTime.now()
        );

        when(chatMessageRepository.findByIdWithChatRoom(chatMessageId))
            .thenReturn(Optional.of(chatMessage));
        when(chatMessageRepository.countByChatRoom(chatRoom))
            .thenReturn(1L);
        when(delayedItemRepository.findById(itemId))
            .thenReturn(Optional.of(delayedItem));

        // when
        listener.handleFirstSent(event);

        // then
        verify(notificationService, never()).createAndSend(
            anyLong(), any(), any(), any(), anyLong(), anyMap()
        );
    }

    // ========== 채팅방 타입 검증 테스트 ==========

    @Test
    @DisplayName("t4: GROUP 채팅방인 경우 알림 전송하지 않음")
    void t4() {
        // given
        Long chatMessageId = 1L;

        ChatRoom chatRoom = mock(ChatRoom.class);
        when(chatRoom.getRoomType()).thenReturn(ChatRoom.RoomType.GROUP); // GROUP 채팅

        ChatMessage chatMessage = mock(ChatMessage.class);
        when(chatMessage.getChatRoom()).thenReturn(chatRoom);

        DirectMessageSentEvent event = new DirectMessageSentEvent(
            chatMessageId, 100L, 2L, "구매자", LocalDateTime.now()
        );

        when(chatMessageRepository.findByIdWithChatRoom(chatMessageId))
            .thenReturn(Optional.of(chatMessage));

        // when
        listener.handleFirstSent(event);

        // then
        verify(notificationService, never()).createAndSend(
            anyLong(), any(), any(), any(), anyLong(), anyMap()
        );
    }
}
