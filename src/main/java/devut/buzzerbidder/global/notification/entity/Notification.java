package devut.buzzerbidder.global.notification.entity;

import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import devut.buzzerbidder.global.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean check;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Builder
    public Notification(
        Long userId,
        NotificationType type,
        String message,
        String resourceType,
        Long resourceId,
        String metadata
    ) {
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.check = false;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.metadata = metadata;
    }

    public void markAsCheck() {
        this.check = true;
    }

}
