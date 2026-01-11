package devut.buzzerbidder.domain.chat.entity;

import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType roomType;

    // 참조하는 엔티티의 채팅방 유형(1:1 또는 경매방 채팅)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceEntityType referenceType;

    // 참조하는 엔티티의 ID
    @Column(nullable = false)
    private Long referenceEntityId;

    @Column
    private Long lastMessageId;

    @Column(length = 200)
    private String lastMessageContent;

    @Column
    private LocalDateTime lastMessageTime;

    @Column(nullable = false)
    private boolean isActive = false;

    @Builder
    public ChatRoom(RoomType roomType, ReferenceEntityType referenceType, Long referenceEntityId, boolean isActive) {
        this.roomType = roomType;
        this.referenceType = referenceType;
        this.referenceEntityId = referenceEntityId;
        this.lastMessageId = null;
        this.isActive = isActive;
    }

    // 채팅방 유형
    public enum RoomType {
        DM,    // 1:1 채팅
        GROUP  // 그룹 채팅(경매방 채팅)
    }

    // 참조 엔티티
    public enum ReferenceEntityType {
        ITEM, // 지연 경매품 엔티티 참조 (지연 경매 1:1 채팅의 경우 참조)
        AUCTION_ROOM, // 경매방 엔티티 참조 (경매방 채팅의 경우 참조)
        LIVE_ITEM // 라이브 경매품 엔티티 참조 (라이브 경매 1:1 DM 채팅의 경우 참조)
    }

    public void updateLastMessage(Long messageId, String content, LocalDateTime time) {
        // null 체크 및 더 최신 ID일 경우에만 업데이트 (순서 보장)
        if (messageId != null && (this.lastMessageId == null || messageId > this.lastMessageId)) {
            this.lastMessageId = messageId;
            this.lastMessageContent = content;
            this.lastMessageTime = time;
        }
    }
}