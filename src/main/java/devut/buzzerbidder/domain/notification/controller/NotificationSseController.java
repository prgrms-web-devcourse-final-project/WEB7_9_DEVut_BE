package devut.buzzerbidder.domain.notification.controller;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.notification.service.SseEmitterPool;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification", description = "알림 API")
public class NotificationSseController {

    private final SseEmitterPool emitterPool;
    private final RequestContext requestContext;

    /**
     * 개인 알림 구독
     */
    @GetMapping(
        value = "/subscribe",
        produces = "text/event-stream")
    @Operation(summary = "알림 구독", description = "SSE를 통해 실시간 알림을 구독합니다.")
    public SseEmitter subscribe() {
        User user = requestContext.getCurrentUser();
        return emitterPool.subscribe("user:" + user.getId());
    }
}
