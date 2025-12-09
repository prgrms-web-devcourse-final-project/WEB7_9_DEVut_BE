package devut.buzzerbidder.global.notification.controller;

import devut.buzzerbidder.global.notification.service.SseEmitterPool;
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

    /**
     * 개인 알림 구독
     */
    @GetMapping("/subscribe")
    public SseEmitter subscribe(Long userId) {
        return emitterPool.subscribe("user:" + userId);
    }
}
