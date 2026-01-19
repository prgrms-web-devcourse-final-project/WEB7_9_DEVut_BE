package devut.buzzerbidder.domain.chat.event.listener;

import devut.buzzerbidder.domain.chat.service.ChatRoomParticipantService;
import devut.buzzerbidder.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketEventListener {

    private final ChatRoomParticipantService participantService;

    /**
     * 입장 감지
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        if (destination != null && destination.startsWith("/receive/chat/auction/")) {

            // id 추출
            String auctionIdStr = destination.substring(destination.lastIndexOf('/') + 1);

            try {
                Long auctionId = Long.parseLong(auctionIdStr);

                // 인증 객체
                Principal principal = headerAccessor.getUser();

                if (principal instanceof UsernamePasswordAuthenticationToken token &&
                        token.getPrincipal() instanceof CustomUserDetails userDetails) {

                    Long userId = userDetails.getUser().getId();

                    // 세션 정보 저장
                    participantService.saveSessionInfo(sessionId, auctionId, userId);

                    log.info("경매방 구독 감지: 방={}, 유저={}, 세션={}", auctionId, userId, sessionId);
                }
            } catch (NumberFormatException e) {
                log.warn("경매방 ID 파싱 실패: {}", destination);
            }
        }
    }

    /**
     * 연결 해제 감지
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("웹소켓 연결 해제 감지: 세션={}", sessionId);

        // 저장해둔 세션 정보를 바탕으로 자동 퇴장 처리
        participantService.handleDisconnect(sessionId);
    }
}