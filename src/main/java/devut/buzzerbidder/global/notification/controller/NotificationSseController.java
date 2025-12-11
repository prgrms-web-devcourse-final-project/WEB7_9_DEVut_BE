package devut.buzzerbidder.global.notification.controller;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.notification.service.SseEmitterPool;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationSseController {

    private final SseEmitterPool emitterPool;
    private final RequestContext requestContext;

    /**
     * 개인 알림 구독
     */
    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        User user = requestContext.getCurrentUser();
        return emitterPool.subscribe("user:" + user.getId());
    }
}
