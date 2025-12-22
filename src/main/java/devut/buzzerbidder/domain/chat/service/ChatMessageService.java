package devut.buzzerbidder.domain.chat.service;

import devut.buzzerbidder.domain.chat.dto.request.AuctionChatMessageRequest;
import devut.buzzerbidder.domain.chat.dto.response.AuctionChatMessageResponse;
import devut.buzzerbidder.domain.chat.entity.ChatRoom;
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

    // 경매방 채팅 브로드캐스트 경로
    private static final String CHAT_PREFIX = "/receive/chat/auction/";

    private String generateTemporaryId() {
        return UUID.randomUUID().toString();
    }

    public void sendAuctionMessage(Long auctionId, User sender, AuctionChatMessageRequest request) {

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

        String destination = CHAT_PREFIX + chatRoom.getId();
        messagingTemplate.convertAndSend(destination, response);
    }

    // TODO: 1대1 채팅 전송
}
