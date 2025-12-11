package devut.buzzerbidder.domain.chat.service;

import devut.buzzerbidder.domain.chat.dto.request.AuctionChatMessageRequest;
import devut.buzzerbidder.domain.chat.dto.response.AuctionChatMessageResponse;
import devut.buzzerbidder.domain.chat.repository.ChatRoomRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 경매방 채팅 경로
    private static final String CHAT_TOPIC_PREFIX = "/topic/chat/auction/";

    private String generateTemporaryId() {
        return UUID.randomUUID().toString();
    }

    public void sendAuctionMessage(Long chatRoomId, User sender, AuctionChatMessageRequest request) {

        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));

        String tempId =  generateTemporaryId();
        LocalDateTime now = LocalDateTime.now();

        AuctionChatMessageResponse response = new AuctionChatMessageResponse(
                tempId,
                sender.getNickname(),
                request.message(),
                now
        );

        String destination = CHAT_TOPIC_PREFIX + chatRoomId;
        messagingTemplate.convertAndSend(destination, response);
    }
}
