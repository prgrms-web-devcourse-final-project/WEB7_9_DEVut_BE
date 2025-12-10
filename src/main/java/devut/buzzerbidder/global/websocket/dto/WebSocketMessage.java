package devut.buzzerbidder.global.websocket.dto;

import devut.buzzerbidder.global.websocket.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "WebSocket 메시지")
public record WebSocketMessage(
    @Schema(description = "메시지 타입")
    MessageType type,

    @Schema(description = "채널 (auction:123, chat:456)", example = "auction:123")
    String channel,

    @Schema(description = "메시지 데이터")
    Object data,

    @Schema(description = "발신자 ID", example = "1")
    Long userId,

    @Schema(description = "타임스탬프")
    Long timestamp
) {
    public static WebSocketMessage of(MessageType type, String channel, Object data, Long userId) {
        return new WebSocketMessage(type, channel, data, userId, System.currentTimeMillis());
    }
}