package devut.buzzerbidder.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record NotificationListResponse(
    @Schema(description = "알림 목록")
    List<NotificationDto> notifications,

    @Schema(description = "전체 알림 개수", example = "15")
    int totalCount,

    @Schema(description = "읽지 않은 알림 개수", example = "5")
    int unreadCount
) {
    public static NotificationListResponse of(
        List<NotificationDto> notifications,
        long unreadCount
    ) {
        return new NotificationListResponse(
            notifications,
            notifications.size(),
            (int) unreadCount
        );
    }
}
