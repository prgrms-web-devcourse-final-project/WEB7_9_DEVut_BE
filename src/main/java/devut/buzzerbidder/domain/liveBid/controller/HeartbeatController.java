package devut.buzzerbidder.domain.liveBid.controller;

import devut.buzzerbidder.domain.liveBid.service.HeartbeatService;
import devut.buzzerbidder.global.security.CustomUserDetails;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class HeartbeatController {

    private final HeartbeatService heartbeatService;

    @MessageMapping("/auction/heartbeat") // 클라이언트는 /send/auction/heartbeat 로 보냄
    public void heartbeat(Principal principal) {
        Long userId = extractUserId(principal);
        heartbeatService.heartbeat(userId);
    }

    private Long extractUserId(Principal principal) {
        Authentication auth = (Authentication) principal;
        CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
        return cud.getUser().getId();
    }
}
