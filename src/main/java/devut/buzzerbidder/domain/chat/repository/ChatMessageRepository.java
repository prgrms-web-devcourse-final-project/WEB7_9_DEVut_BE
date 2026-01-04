package devut.buzzerbidder.domain.chat.repository;

import devut.buzzerbidder.domain.chat.entity.ChatMessage;
import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomOrderByCreateDateAsc(ChatRoom chatRoom);

    long countByChatRoom(ChatRoom chatRoom);

    @Query("SELECT cm FROM ChatMessage cm JOIN FETCH cm.chatRoom WHERE cm.id = :id")
    Optional<ChatMessage> findByIdWithChatRoom(@Param("id") Long id);
}
