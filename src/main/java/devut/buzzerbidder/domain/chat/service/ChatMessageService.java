package devut.buzzerbidder.domain.chat.service;

import devut.buzzerbidder.domain.chat.dto.request.ChatMessageRequest;
import devut.buzzerbidder.domain.chat.dto.response.AuctionChatMessageResponse;
import devut.buzzerbidder.domain.chat.dto.response.DirectMessageResponse;
import devut.buzzerbidder.domain.chat.entity.ChatMessage;
import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import devut.buzzerbidder.domain.chat.repository.ChatMessageRepository;
import devut.buzzerbidder.domain.chat.repository.ChatRoomEnteredRepository;
import devut.buzzerbidder.domain.chat.repository.ChatRoomRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 경매방 채팅 브로드캐스트 경로
    private static final String AUCTION_DESTINATION_PREFIX = "/receive/chat/auction/";
    private static final String DM_DESTINATION_PREFIX = "/receive/chat/dm/";
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomEnteredRepository chatRoomEnteredRepository;

    private String generateTemporaryId() {
        return UUID.randomUUID().toString();
    }

    public void sendAuctionMessage(Long auctionId, User sender, ChatMessageRequest request) {

        ChatRoom chatRoom = chatRoomRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));

        String tempId =  generateTemporaryId();
        String message = request.content();
        LocalDateTime now = LocalDateTime.now();


        AuctionChatMessageResponse response = new AuctionChatMessageResponse(
                tempId,
                sender.getProfileImageUrl(),
                sender.getNickname(),
                message,
                now
        );

        String destination = AUCTION_DESTINATION_PREFIX + chatRoom.getId();
        messagingTemplate.convertAndSend(destination, response);
    }

    @Transactional
    public void sendDirectMessage(Long chatRoomId, User sender, ChatMessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));

        // 해당 채팅방의 참여자가 아닐 시 예외처리
        boolean isParticipant = chatRoomEnteredRepository.
                existsByUserAndChatRoom(sender, chatRoom);
        if (!isParticipant) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .message(request.content())
                .build();
        chatMessageRepository.save(chatMessage);

        DirectMessageResponse response = new DirectMessageResponse(
                chatMessage.getId(),
                sender.getProfileImageUrl(),
                sender.getNickname(),
                chatMessage.getMessage(),
                chatMessage.getCreateDate()
        );

        String destination = DM_DESTINATION_PREFIX + chatRoomId;
        messagingTemplate.convertAndSend(destination, response);
    }
}
