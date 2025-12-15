package devut.buzzerbidder.domain.chat.repository;

import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import devut.buzzerbidder.domain.chat.entity.ChatRoomEntered;
import devut.buzzerbidder.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomEnteredRepository extends JpaRepository<ChatRoomEntered, Long> {

    Optional<ChatRoomEntered> findByUserAndChatRoom(User user, ChatRoom chatRoom);
}
