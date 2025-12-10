package devut.buzzerbidder.global.notification.dto;

import devut.buzzerbidder.global.notification.entity.Notification;
import devut.buzzerbidder.global.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record NotificationDto(
    @Schema(description = "알림 ID", example = "1")
    Long id,

    @Schema(description = "사용자 ID", example = "123")
    Long userId,

    @Schema(description = "알림 타입")
    NotificationType type,

    @Schema(description = "알림 메시지", example = "경매가 시작되었습니다.")
    String message,

    @Schema(description = "읽음 여부", example = "false")
    boolean check,

    @Schema(description = "생성 일자")
    LocalDateTime createDate
) {
    public static NotificationDto from(Notification notification) {
        return new NotificationDto(
            notification.getId(),
            notification.getUserId(),
            notification.getType(),
            notification.getMessage(),
            notification.isCheck(),
            notification.getCreateDate()
        );
    }
}
