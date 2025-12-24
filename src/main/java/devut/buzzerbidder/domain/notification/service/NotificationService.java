package devut.buzzerbidder.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.domain.notification.dto.NotificationDto;
import devut.buzzerbidder.domain.notification.entity.Notification;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationMessageBroker messageBroker;
    private final ObjectMapper objectMapper;

    /**
     * 개인 알림 생성 및 발송
     *
     * @param userId 사용자 ID
     * @param type 알림 타입
     * @param message 알림 메시지
     * @param resourceType 리소스 타입 (선택, 예: "DELAYED_ITEM")
     * @param resourceId 리소스 ID (선택)
     * @param metadata 추가 메타데이터 (선택)
     */
    @Transactional(readOnly = false)
    public Notification createAndSend(
        long userId,
        NotificationType type,
        String message,
        String resourceType,
        Long resourceId,
        Map<String, Object> metadata
    ) {
        // metadata를 JSON 문자열로 변환
        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (Exception e) {
                // JSON 반환 실패 시 무시
            }
        }

        // 1. DB에 알림 저장
        Notification notification = repository.save(
            Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .metadata(metadataJson)
                .build()
        );

        // 2. 개인 채널에만 발송
        messageBroker.publishToUser(userId, NotificationDto.from(notification));

        return notification;
    }

    /**
     * 여러 사용자에게 동시 알림 발송
     */
    @Transactional(readOnly = false)
    public List<Notification> createAndSendToMultiple(
        List<Long> userIds,
        NotificationType type,
        String message,
        String resourceType,
        Long resourceId,
        Map<String, Object> metadata
    ) {
        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (Exception e) {
                // JSON 변환 실패 시 무시
            }
        }

        // 1. Bulk insert로 DB 저장
        String finalMetadataJson = metadataJson;
        List<Notification> notifications = userIds.stream()
            .map(userId -> Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .metadata(finalMetadataJson)
                .build())
            .toList();

        List<Notification> saved = repository.saveAll(notifications);

        // 2. 채널 리스트 만들어서 한번에 발송
        List<String> channels = userIds.stream()
            .map(id -> "user:" + id)
            .toList();

        messageBroker.publishToChannels(channels, NotificationDto.from(saved.get(0)));

        return saved;
    }

    /**
     * 사용자의 모든 알림 조회
     */
    public List<NotificationDto> getNotifications(Long userId) {
        return repository.findByUserIdOrderByCreateDateDesc(userId)
            .stream()
            .map(NotificationDto::from)
            .toList();
    }

    /**
     * 사용자의 읽지 않은 알림 조회
     */
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        return repository.findByUserIdAndIsCheckedFalseOrderByCreateDateDesc(userId)
            .stream()
            .map(NotificationDto::from)
            .toList();
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    public Long getUnreadCount(Long userId) {
        return repository.countByUserIdAndIsCheckedFalse(userId);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional(readOnly = false)
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = repository.findById(notificationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        notification.markAsChecked();
        repository.save(notification);
    }

    /**
     * 알림 삭제
     */
    @Transactional(readOnly = false)
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = repository.findById(notificationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        repository.delete(notification);
    }
}
