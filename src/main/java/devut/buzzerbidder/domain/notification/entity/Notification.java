package devut.buzzerbidder.domain.notification.entity;

import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(
            name = "idx_notification_user_check_date",
            columnList = "user_id, is_checked, create_date DESC"
        ),
        @Index(
            name = "idx_notification_reminder_check",
            columnList = "user_id, type, resource_id"
        )
    })
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

    @Column(name = "is_checked", nullable = false)
    private boolean isChecked;

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
        this.isChecked = false;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.metadata = metadata;
    }

    public void markAsChecked() {
        this.isChecked = true;
    }

}
