package devut.buzzerbidder.global.websocket1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionPool {

    private final ObjectMapper objectMapper;

    // 채널별 세션 관리: "auction:123" -> Set<WebSocketSession>
    private final Map<String, Set<WebSocketSession>> channelSessions = new ConcurrentHashMap<>();

    // 세션별 사용자 정보: sessionId -> userId
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    // 세션별 구독 채널: sessionId -> Set<channel>
    private final Map<String, Set<String>> sessionChannels = new ConcurrentHashMap<>();

    /**
     * 채널 구독
     */
    public void subscribe(WebSocketSession session, String channel, Long userId) {
        channelSessions.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet())
            .add(session);

        sessionUserMap.put(session.getId(), userId);

        sessionChannels.computeIfAbsent(session.getId(), k -> ConcurrentHashMap.newKeySet())
            .add(channel);

        log.info("Session {} subscribed to channel: {}", session.getId(), channel);
    }

    /**
     * 채널 구독 해제
     */
    public void unsubscribe(WebSocketSession session, String channel) {
        Set<WebSocketSession> sessions = channelSessions.get(channel);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                channelSessions.remove(channel);
            }
        }

        Set<String> channels = sessionChannels.get(session.getId());
        if (channels != null) {
            channels.remove(channel);
        }

        log.info("Session {} unsubscribed from channel: {}", session.getId(), channel);
    }

    /**
     * 세션 연결 해제 시 모든 채널에서 제거
     */
    public void removeSession(WebSocketSession session) {
        String sessionId = session.getId();

        // 구독한 모든 채널에서 제거
        Set<String> channels = sessionChannels.get(sessionId);
        if (channels != null) {
            channels.forEach(channel -> {
                Set<WebSocketSession> sessions = channelSessions.get(channel);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        channelSessions.remove(channel);
                    }
                }
            });
            sessionChannels.remove(sessionId);
        }

        sessionUserMap.remove(sessionId);
        log.info("Session {} removed from all channels", sessionId);
    }

    /**
     * 특정 채널의 모든 세션에게 메시지 전송
     */
    public void sendToChannel(String channel, Object message) {
        Set<WebSocketSession> sessions = channelSessions.get(channel);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize message", e);
            return;
        }

        sessions.forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (IOException e) {
                    log.error("Failed to send message to session {}", session.getId(), e);
                    removeSession(session);
                }
            }
        });
    }

    /**
     * 특정 세션에게만 메시지 전송
     */
    public void sendToSession(WebSocketSession session, Object message) {
        if (!session.isOpen()) {
            return;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        } catch (Exception e) {
            log.error("Failed to send message to session {}", session.getId(), e);
        }
    }

    public Long getUserId(WebSocketSession session) {
        return sessionUserMap.get(session.getId());
    }
}
