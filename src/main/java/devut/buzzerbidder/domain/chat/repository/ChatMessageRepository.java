package devut.buzzerbidder.domain.chat.repository;

import devut.buzzerbidder.domain.chat.entity.ChatMessage;
import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomOrderByCreateDateAsc(ChatRoom chatRoom);
}
