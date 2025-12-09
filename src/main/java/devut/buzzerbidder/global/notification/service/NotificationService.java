package devut.buzzerbidder.global.notification.service;

import devut.buzzerbidder.global.notification.dto.NotificationDto;
import devut.buzzerbidder.global.notification.entity.Notification;
import devut.buzzerbidder.global.notification.enums.NotificationType;
import devut.buzzerbidder.global.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationMessageBroker messageBroker;

    /**
     * 알림을 생성 및 발송 (채널 지정 가능)
     *
     * @param userId 알림을 받을 사용자 ID
     * @param type 알림 타입
     * @param message 알림 메시지
     * @param channels 발송할 채널 목록 (예: ["user:123", "auction:456"])
     * @return 저장된 알림 엔티티
     */
    public Notification createAndSend(long userId, NotificationType type, String message, List<String> channels) {
        // 1. DB에 알림 저장
        Notification notification = repository.save(
                Notification.builder()
                    .userId(userId)
                    .type(type)
                    .message(message)
                    .build()
        );

        // 2. DTO 변환
        NotificationDto notificationDto = NotificationDto.from(notification);

        // 3. Redis Pub/Sub으로 브로드캐스트 (모든 서버가 수신)
        messageBroker.publishToChannels(channels, notificationDto);

        return notification;
    }

    /**
     * 개인 알림만
     */
    public Notification createAndSend(long userId, NotificationType type, String message) {
        return createAndSend(userId, type, message, List.of("user:" + userId));
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
        return repository.findByUserIdAndCheckFalseOrderByCreateDateDesc(userId)
            .stream()
            .map(NotificationDto::from)
            .toList();
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    public Long getUnreadCount(Long userId) {
        return repository.countByUserIdAndCheckFalse(userId);
    }

    /**
     * 알림 읽음 처리
     */
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = repository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to mark this notification");
        }

        notification.markAsCheck();
        repository.save(notification);
    }

    /**
     * 알림 삭제
     */
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = repository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to delete this notification");
        }

        repository.delete(notification);
    }
}
