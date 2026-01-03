package devut.buzzerbidder.domain.notification.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devut.buzzerbidder.domain.notification.entity.Notification;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

public record NotificationDto(
    @Schema(description = "알림 ID", example = "1")
    Long id,

    @Schema(description = "사용자 ID", example = "123")
    Long userId,

    @Schema(description = "알림 타입")
    NotificationType type,

    @Schema(description = "알림 메시지", example = "경매가 시작되었습니다.")
    String message,

    @Schema(description = "읽음 여부", example = "false")
    boolean isChecked,

    @Schema(description = "생성 일자")
    LocalDateTime createDate,

    @Schema(description = "관련 리소스 타입 (LIVEITEM: 상품, AUCTION: 경매, CHATROOMS: 채팅)", nullable = true)
    String resourceType,

    @Schema(description = "관련 리소스 ID (알림 클릭 시 이동할 대상의 ID)", nullable = true)
    Long resourceId,

    @Schema(
        description = "추가 메타데이터 (발신자 정보, 금액 등)",
        example = "{\"senderId\": 7892, \"senderName\": \"USER_7892\", \"amount\": 45000}",
        nullable = true
    )
    Map<String, Object> metadata
) {
    public static NotificationDto from(Notification notification) {
        Map<String, Object> metadata = null;
        if (notification.getMetadata() != null && !notification.getMetadata().isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                metadata = mapper.readValue(
                    notification.getMetadata(),
                    new TypeReference<Map<String, Object>>() {}
                );
            } catch (Exception e) {
                // JSON 파싱 실패 시 null
            }
        }
        return new NotificationDto(
            notification.getId(),
            notification.getUserId(),
            notification.getType(),
            notification.getMessage(),
            notification.isChecked(),
            notification.getCreateDate(),
            notification.getResourceType(),
            notification.getResourceId(),
            metadata
        );
    }
}
