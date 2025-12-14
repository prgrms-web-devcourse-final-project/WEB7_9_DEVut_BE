package devut.buzzerbidder.domain.chat.service;

import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import devut.buzzerbidder.domain.chat.entity.ChatRoomEntered;
import devut.buzzerbidder.domain.chat.repository.ChatRoomRepository;
import devut.buzzerbidder.domain.chat.repository.ChatRoomEnteredRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomEnteredRepository chatRoomEnteredRepository;

    // 특정 경매 ID에 해당하는 채팅방을 조회하거나, 존재하지 않으면 생성
    public ChatRoom getOrCreateAuctionChatRoom(Long auctionId) {

        Optional<ChatRoom> existingChatRoom = chatRoomRepository.findByAuctionId(auctionId);

        if (existingChatRoom.isPresent()) {
            return existingChatRoom.get();
        }

        ChatRoom newRoom = ChatRoom.builder()
                .roomType(ChatRoom.RoomType.GROUP)
                .referenceType(ChatRoom.ReferenceEntityType.AUCTION_ROOM)
                .referenceEntityId(auctionId)
                .isActive(true)
                .build();

        return chatRoomRepository.save(newRoom);
    }

    // 채팅방 참여 상태 관리
    public void enterChatRoom(User user, ChatRoom chatRoom) {

        chatRoomEnteredRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseGet(() -> {
                    ChatRoomEntered newEntry = new ChatRoomEntered(user, chatRoom);

                    return chatRoomEnteredRepository.save(newEntry);
                });
    }

    public void exitAuctionChatRoom(Long auctionId, User user) {

        ChatRoom chatRoom = chatRoomRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));

        ChatRoomEntered entered = chatRoomEnteredRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT));

        chatRoomEnteredRepository.delete(entered);
    }

    // TODO: 1대1 채팅 inActive 활용
}

