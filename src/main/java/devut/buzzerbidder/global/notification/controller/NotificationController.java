package devut.buzzerbidder.global.notification.controller;

import devut.buzzerbidder.domain.member.entity.Member;
import devut.buzzerbidder.global.notification.dto.NotificationDto;
import devut.buzzerbidder.global.notification.service.NotificationService;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import devut.buzzerbidder.global.response.ApiResponse;
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
public class NotificationController {

    private final NotificationService notificationService;
    private final RequestContext requestContext;

    @GetMapping
    public ApiResponse<List<NotificationDto>> getNotifications() {
        Member member = requestContext.getCurrentMember();
        List<NotificationDto> notifications = notificationService.getNotifications(member.getId());
        return ApiResponse.ok("알림 목록을 조회했습니다.", notifications);
    }

    @GetMapping("/unread")
    public ApiResponse<List<NotificationDto>> getUnreadNotifications() {
        Member member = requestContext.getCurrentMember();
        List<NotificationDto> notifications = notificationService.getUnreadNotifications(member.getId());
        return ApiResponse.ok("읽지 않은 알림을 조회했습니다.", notifications);
    }

    @GetMapping("/unread/count")
    public ApiResponse<Long> getUnreadCount() {
        Member member = requestContext.getCurrentMember();
        Long count = notificationService.getUnreadCount(member.getId());
        return ApiResponse.ok("읽지 않은 알림 개수를 조회했습니다.", count);
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        Member member = requestContext.getCurrentMember();
        notificationService.markAsRead(id, member.getId());
        return ApiResponse.ok("알림을 읽음 처리했습니다.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id) {
        Member member = requestContext.getCurrentMember();
        notificationService.deleteNotification(id, member.getId());
        return ApiResponse.ok("알림을 삭제했습니다.");
    }

}
