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

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean check;

    @Builder
    public Notification(Long userId, NotificationType type, String message) {
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.check = false;
    }

    public void markAsCheck() {
        this.check = true;
    }

}
