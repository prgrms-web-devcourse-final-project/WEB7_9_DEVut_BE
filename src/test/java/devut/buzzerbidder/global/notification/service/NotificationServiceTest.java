package devut.buzzerbidder.global.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.notification.dto.NotificationDto;
import devut.buzzerbidder.global.notification.entity.Notification;
import devut.buzzerbidder.global.notification.enums.NotificationType;
import devut.buzzerbidder.global.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationMessageBroker messageBroker;

    @InjectMocks
    private NotificationService notificationService;

    // ========== 알림 생성 및 발송 테스트 ==========

    @Test
    @DisplayName("t1: 개인 알림 생성 및 발송 성공")
    void t1() {
        //given
        Long memberId = 1L;
        NotificationType type = NotificationType.AUCTION_WIN;
        String message = "낙찰되었습니다!";

        Notification notification = Notification.builder()
                .memberId(memberId)
                .type(type)
                .message(message)
                .build();

        given(repository.save(any(Notification.class))).willReturn(notification);

        //when
        Notification result = notificationService.createAndSend(memberId, type, message);

        //then
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getType()).isEqualTo(type);
        assertThat(result.getMessage()).isEqualTo(message);

        verify(repository).save(any(Notification.class));
    }

    // ========== 알림 조회 테스트 ==========

    @Test
    @DisplayName("t2: 읽지 않은 알림 조회 성공")
    void t2() {
        // given
        Long memberId = 1L;
        List<Notification> notifications = List.of(
            Notification.builder()
                .memberId(memberId)
                .type(NotificationType.AUCTION_OUTBID)
                .message("입찰이 밀렸습니다.")
                .build(),
            Notification.builder()
                .memberId(memberId)
                .type(NotificationType.AUCTION_START)
                .message("경매가 시작되었습니다.")
                .build()
        );

        given(repository.findByMemberIdAndCheckFalseOrderByCreateDateDesc(memberId))
            .willReturn(notifications);

        // when
        List<NotificationDto> result = notificationService.getUnreadNotifications(memberId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).userId()).isEqualTo(memberId);
        assertThat(result.get(1).userId()).isEqualTo(memberId);
    }

    @Test
    @DisplayName("t3: 읽지 않은 알림 개수 조회 성공")
    void t3() {
        Long memberId = 1L;
        Long expectedCount = 5L;

        given(repository.countByMemberIdAndCheckFalse(memberId)).willReturn(expectedCount);

        //when
        Long result = notificationService.getUnreadCount(memberId);

        //then
        assertThat(result).isEqualTo(expectedCount);
    }

    // ========== 알림 읽음 처리 테스트 ==========

    @Test
    @DisplayName("t4: 알림 읽음 처리 성공")
    void t4() {
        // given
        Long notificationId = 1L;
        Long memberId = 1L;

        Notification notification = Notification.builder()
            .memberId(memberId)
            .type(NotificationType.AUCTION_WIN)
            .message("낙찰되었습니다!")
            .build();

        given(repository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        notificationService.markAsRead(notificationId, memberId);

        // then
        assertThat(notification.isCheck()).isTrue();
    }

    @Test
    @DisplayName("t5: 알림 읽음 처리 실패 - 알림 없음")
    void t5() {
        // given
        Long notificationId = 999L;
        Long memberId = 1L;

        given(repository.findById(notificationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, memberId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("t6: 알림 읽음 처리 실패 - 권한 없음")
    void t6() {
        // given
        Long notificationId = 1L;
        Long ownerId = 1L;
        Long otherUserId = 2L;

        Notification notification = Notification.builder()
            .memberId(ownerId)
            .type(NotificationType.AUCTION_WIN)
            .message("낙찰되었습니다!")
            .build();

        given(repository.findById(notificationId)).willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, otherUserId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_FORBIDDEN);
    }

    // ========== 알림 삭제 테스트 ==========

    @Test
    @DisplayName("t7: 알림 삭제 성공")
    void t7() {
        // given
        Long notificationId = 1L;
        Long memberId = 1L;

        Notification notification = Notification.builder()
            .memberId(memberId)
            .type(NotificationType.AUCTION_END)
            .message("경매가 종료되었습니다.")
            .build();

        given(repository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        notificationService.deleteNotification(notificationId, memberId);

        // then
        verify(repository).delete(notification);
    }

    @Test
    @DisplayName("t8: 알림 삭제 실패 - 알림 없음")
    void t8() {
        // given
        Long notificationId = 999L;
        Long memberId = 1L;

        given(repository.findById(notificationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.deleteNotification(notificationId, memberId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("t9: 알림 삭제 실패 - 권한 없음")
    void t9() {
        // given
        Long notificationId = 1L;
        Long ownerId = 1L;
        Long otherUserId = 2L;

        Notification notification = Notification.builder()
            .memberId(ownerId)
            .type(NotificationType.AUCTION_WIN)
            .message("낙찰되었습니다!")
            .build();

        given(repository.findById(notificationId)).willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() -> notificationService.deleteNotification(notificationId, otherUserId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_FORBIDDEN);
    }


}
