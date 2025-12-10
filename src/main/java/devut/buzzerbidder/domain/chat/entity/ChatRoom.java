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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType roomType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferenceEntityType referenceType;

    @Column(nullable = false)
    private Long referenceEntityId;

    private boolean isActive = false;

    @Builder
    public ChatRoom(RoomType roomType, Long referenceEntityId, boolean isActive) {
        this.roomType = roomType;
        this.referenceEntityId = referenceEntityId;
        this.isActive = isActive;
    }

    // 채팅방 유형
    public enum RoomType {
        DM,    // 1:1 채팅
        AUCTION  // 경매방 채팅(그룹 채팅)
    }

    // 참조 엔티티
    public enum ReferenceEntityType {
        ITEM, // 경매품 엔티티 참조 (1:1 채팅의 경우 참조)
        AUCTION_ROOM // 경매방 엔티티 참조 (경매방 채팅의 경우 참조)
    }

}