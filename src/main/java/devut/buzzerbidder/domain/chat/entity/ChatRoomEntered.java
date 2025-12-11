package devut.buzzerbidder.domain.chat.entity;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userID", "chatroomID"})
    }
)
public class ChatRoomEntered extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "userID",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_CHAT_ROOM_ENTERED_USER") // <--- 이 부분 추가
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "chatroomID",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_CHAT_ROOM_ENTERED_CHATROOM") // <--- 이 부분 추가
    )
    private ChatRoom chatRoom;

    @Column(nullable = false)
    private Long lastReadMessageID = 0L;


    public ChatRoomEntered(User user, ChatRoom chatRoom) {
        this.user = user;
        this.chatRoom = chatRoom;
    }


    public void updateReadStatus(Long messageId) {
        if (messageId != null && messageId > this.lastReadMessageID) {
            this.lastReadMessageID = messageId;
        }
    }
}
