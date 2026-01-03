package devut.buzzerbidder.domain.notification.controller;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.notification.dto.NotificationDto;
import devut.buzzerbidder.domain.notification.dto.NotificationListResponse;
import devut.buzzerbidder.domain.notification.service.NotificationService;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;
    private final RequestContext requestContext;

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "현재 사용자의 모든 알림 목록을 조회합니다.")
    public ApiResponse<NotificationListResponse> getNotifications() {
        User user = requestContext.getCurrentUser();
        List<NotificationDto> notifications = notificationService.getNotifications(user.getId());
        Long unreadCount = notificationService.getUnreadCount(user.getId());

        return ApiResponse.ok(
            "알림 목록을 조회했습니다.",
            NotificationListResponse.of(notifications, unreadCount)
        );
    }

    @GetMapping("/unread")
    @Operation(summary = "읽지 않은 알림 조회", description = "현재 사용자의 읽지 않은 알림 목록을 조회합니다.")
    public ApiResponse<NotificationListResponse> getUnreadNotifications() {
        User user = requestContext.getCurrentUser();
        List<NotificationDto> notifications = notificationService.getUnreadNotifications(user.getId());
        Long unreadCount = notificationService.getUnreadCount(user.getId());

        return ApiResponse.ok(
            "읽지 않은 알림을 조회했습니다.",
            NotificationListResponse.of(notifications, unreadCount)
        );
    }

    @GetMapping("/unread/count")
    @Operation(summary = "읽지 않은 알림 개수 조회", description = "현재 사용자의 읽지 않은 알림 개수를 조회합니다.")
    public ApiResponse<Long> getUnreadCount() {
        User user = requestContext.getCurrentUser();
        Long count = notificationService.getUnreadCount(user.getId());

        return ApiResponse.ok("읽지 않은 알림 개수를 조회했습니다.", count);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        User user = requestContext.getCurrentUser();
        notificationService.markAsRead(id, user.getId());

        return ApiResponse.ok("알림을 읽음 처리했습니다.");
    }

    @PatchMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리", description = "현재 사용자의 읽지 않은 모든 알림을 읽음 처리합니다.")
    public ApiResponse<Void> markAllAsRead() {
        User user = requestContext.getCurrentUser();
        int updatedCount = notificationService.markAllAsRead(user.getId());

        return ApiResponse.ok(updatedCount + "개의 알림을 읽음 처리했습니다.");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id) {
        User user = requestContext.getCurrentUser();
        notificationService.deleteNotification(id, user.getId());

        return ApiResponse.ok("알림을 삭제했습니다.");
    }

}
