package devut.buzzerbidder.global.notification.service;

import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.notification.dto.NotificationDto;
import devut.buzzerbidder.global.notification.entity.Notification;
import devut.buzzerbidder.global.notification.enums.NotificationType;
import devut.buzzerbidder.global.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationMessageBroker messageBroker;

    /**
     *  개인 알림 발송
     */
    @Transactional
    public Notification createAndSend(long userId, NotificationType type, String message) {
        return createAndSend(userId, type, message, null, null);
    }

    /**
     * 개인 알림 발송 (리소스 정보 포함)
     */
    @Transactional
    public Notification createAndSend(long userId, NotificationType type,
        String message, String resourceType, Long resourceId) {
        // 1. DB에 알림 저장
        Notification notification = repository.save(
            Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build()
        );

        // 2. 개인 채널에만 발송
        messageBroker.publishToUser(userId, NotificationDto.from(notification));

        return notification;
    }

    /**
     * 여러 사용자에게 동시 알림 발송
     */
    @Transactional
    public List<Notification> createAndSendToMultiple(
        List<Long> userIds,
        NotificationType type,
        String message,
        String resourceType,
        Long resourceId
    ) {
        // 1. Bulk insert로 DB 저장
        List<Notification> notifications = userIds.stream()
            .map(userId -> Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
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
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = repository.findById(notificationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        notification.markAsCheck();
        repository.save(notification);
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = repository.findById(notificationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        repository.delete(notification);
    }
}
