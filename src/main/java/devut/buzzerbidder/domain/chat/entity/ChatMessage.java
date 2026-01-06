package devut.buzzerbidder.domain.chat.entity;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "senderID",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_CHAT_MESSAGE_SENDER")
    )
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "chatroomID",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_CHAT_MESSAGE_CHATROOM")
    )
    private ChatRoom chatRoom;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Builder
    public ChatMessage(User sender, ChatRoom chatRoom, String message) {
        this.sender = sender;
        this.chatRoom = chatRoom;
        this.message = message;
    }
}
