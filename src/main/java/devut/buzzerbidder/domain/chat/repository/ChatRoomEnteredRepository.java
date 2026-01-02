package devut.buzzerbidder.domain.chat.repository;

import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import devut.buzzerbidder.domain.chat.entity.ChatRoomEntered;
import devut.buzzerbidder.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomEnteredRepository extends JpaRepository<ChatRoomEntered, Long> {

    Optional<ChatRoomEntered> findByUserAndChatRoom(User user, ChatRoom chatRoom);

    boolean existsByUserAndChatRoom(User user, ChatRoom chatRoom);

    // 유저가 참여 중인 DM 방 목록을 최신 메시지 순으로 조회
    @Query("""
        SELECT cre FROM ChatRoomEntered cre 
        JOIN FETCH cre.chatRoom cr 
        WHERE cre.user = :user 
          AND cr.roomType = 'DM' 
        ORDER BY cr.lastMessageId DESC
    """)
    List<ChatRoomEntered> findAllMyDmEntries(@Param("user") User user);

    // 여러 채팅방의 상대방 입장 정보를 한꺼번에 조회(me가 null이면 모든 유저 조회)
    @Query("""
        SELECT cre FROM ChatRoomEntered cre 
        JOIN FETCH cre.user 
        WHERE cre.chatRoom IN :chatRooms 
          AND (:me IS NULL OR cre.user != :me)
    """)
    List<ChatRoomEntered> findCounterparts(@Param("chatRooms") List<ChatRoom> chatRooms, @Param("me") User me);
}
