package devut.buzzerbidder.global.websocket.handler;

import static devut.buzzerbidder.global.websocket.enums.MessageType.AUCTION_BID;
import static devut.buzzerbidder.global.websocket.enums.MessageType.CHAT_MESSAGE;
import static devut.buzzerbidder.global.websocket.enums.MessageType.USER_JOINED;

import com.fasterxml.jackson.databind.ObjectMapper;
import devut.buzzerbidder.global.websocket.dto.WebSocketMessage;
import devut.buzzerbidder.global.websocket.enums.MessageType;
import devut.buzzerbidder.global.websocket.service.WebSocketSessionPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionPool sessionPool;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());

        // TODO: 쿼리 파라미터에서 userId, auctionId 추출
        // String userId = extractUserId(session);
        // String auctionId = extractAuctionId(session);
        // sessionPool.subscribe(session, "auction:" + auctionId, Long.parseLong(userId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received message: {}", payload);

        try {
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);

            // 메시지 타입별 처리
            switch (wsMessage.type()) {
                case AUCTION_BID -> handleAuctionBid(session, wsMessage);
                case CHAT_MESSAGE -> handleChatMessage(session, wsMessage);
                case USER_JOINED -> handleUserJoined(session, wsMessage);
                // TODO: 다른 타입 추가
                default -> log.warn("Unknown message type: {}", wsMessage.type());
            }
        } catch (Exception e) {
            log.error("Failed to handle message", e);
            sendError(session, "Invalid message format");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
        sessionPool.removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: {}", session.getId(), exception);
        sessionPool.removeSession(session);
    }

    // ========== 메시지 타입별 처리 메서드 ==========

    private void handleAuctionBid(WebSocketSession session, WebSocketMessage message) {
        // TODO: 팀원이 경매 로직 작성 시 구현
        log.info("Handling auction bid: {}", message);
    }

    private void handleChatMessage(WebSocketSession session, WebSocketMessage message) {
        // TODO: 팀원이 채팅 로직 작성 시 구현
        log.info("Handling chat message: {}", message);
    }

    private void handleUserJoined(WebSocketSession session, WebSocketMessage message) {
        // TODO: 사용자 입장 처리
        log.info("User joined: {}", message);
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        WebSocketMessage error = WebSocketMessage.of(
            MessageType.ERROR,
            null,
            errorMessage,
            null
        );
        sessionPool.sendToSession(session, error);
    }
}
