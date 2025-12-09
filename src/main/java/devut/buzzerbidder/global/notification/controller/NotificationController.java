package devut.buzzerbidder.global.notification.controller;

import devut.buzzerbidder.global.notification.dto.NotificationDto;
import devut.buzzerbidder.global.notification.service.NotificationService;
import devut.buzzerbidder.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationDto>> getNotifications(@RequestParam Long userId) {
        List<NotificationDto> notifications = notificationService.getNotifications(userId);
        return ApiResponse.ok("알림 목록을 조회했습니다.", notifications);
    }

    @GetMapping("/unread")
    public ApiResponse<List<NotificationDto>> getUnreadNotifications(@RequestParam Long userId) {
        List<NotificationDto> notifications = notificationService.getUnreadNotifications(userId);
        return ApiResponse.ok("읽지 않은 알림을 조회했습니다.", notifications);
    }

    @GetMapping("/unread/count")
    public ApiResponse<Long> getUnreadCount(@RequestParam Long userId) {
        Long count = notificationService.getUnreadCount(userId);
        return ApiResponse.ok("읽지 않은 알림 개수를 조회했습니다.", count);
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id, @RequestParam Long userId) {
        notificationService.markAsRead(id, userId);
        return ApiResponse.ok("알림을 읽음 처리했습니다.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id, @RequestParam Long userId) {
        notificationService.deleteNotification(id, userId);
        return ApiResponse.ok("알림을 삭제했습니다.");
    }

}
