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
     * 개인 알림 발송
     */
    @Transactional
    public Notification createAndSend(long memberId, NotificationType type, String message) {
        // 1. DB에 알림 저장
        Notification notification = repository.save(
            Notification.builder()
                .memberId(memberId)
                .type(type)
                .message(message)
                .build()
        );

        // 2. 개인 채널에만 발송
        messageBroker.publishToUser(memberId, NotificationDto.from(notification));

        return notification;
    }

    /**
     * 사용자의 모든 알림 조회
     */
    public List<NotificationDto> getNotifications(Long memberId) {
        return repository.findByMemberIdOrderByCreateDateDesc(memberId)
            .stream()
            .map(NotificationDto::from)
            .toList();
    }

    /**
     * 사용자의 읽지 않은 알림 조회
     */
    public List<NotificationDto> getUnreadNotifications(Long memberId) {
        return repository.findByMemberIdAndCheckFalseOrderByCreateDateDesc(memberId)
            .stream()
            .map(NotificationDto::from)
            .toList();
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    public Long getUnreadCount(Long memberId) {
        return repository.countByMemberIdAndCheckFalse(memberId);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = repository.findById(notificationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        notification.markAsCheck();
        repository.save(notification);
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long memberId) {
        Notification notification = repository.findById(notificationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        repository.delete(notification);
    }
}
