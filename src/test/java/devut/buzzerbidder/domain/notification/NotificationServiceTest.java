package devut.buzzerbidder.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.domain.notification.dto.NotificationDto;
import devut.buzzerbidder.domain.notification.entity.Notification;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.repository.NotificationRepository;
import devut.buzzerbidder.domain.notification.service.NotificationMessageBroker;
import devut.buzzerbidder.domain.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
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

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationService notificationService;

    // ========== 알림 생성 및 발송 테스트 ==========

    @Test
    @DisplayName("t1: 개인 알림 생성 및 발송 성공")
    void t1() throws Exception {
        //given
        Long userId = 1L;
        NotificationType type = NotificationType.DELAYED_BID_OUTBID;
        String message = "입찰가가 밀렸습니다.";
        String resourceType = "DELAYED_ITEM";
        Long resourceID = 100L;
        Map<String, Object> metadata = Map.of(
            "newBidAmount", 10000L,
            "newBidderUserId", 2L
        );

        Notification notification = Notification.builder()
            .userId(userId)
            .type(type)
            .message(message)
            .resourceType(resourceType)
            .resourceId(resourceID)
            .metadata("{\"newBidAmount\":10000,\"newBidderUserId\":2}")
            .build();

        given(repository.save(any(Notification.class))).willReturn(notification);
        given(objectMapper.writeValueAsString(any(Map.class)))
            .willReturn("{\"newBidAmount\":10000,\"newBidderUserId\":2}");

        //when
        Notification result = notificationService.createAndSend(
            userId, type, message, resourceType, resourceID, metadata);

        //then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getType()).isEqualTo(type);
        assertThat(result.getMessage()).isEqualTo(message);
        assertThat(result.getResourceType()).isEqualTo(resourceType);
        assertThat(result.getResourceId()).isEqualTo(resourceID);
        assertThat(result.getMetadata()).isNotNull();

        verify(repository).save(any(Notification.class));
        verify(messageBroker).publishToUser(any(Long.class), any(NotificationDto.class));
    }

    // ========== 알림 조회 테스트 ==========

    @Test
    @DisplayName("t2: 읽지 않은 알림 조회 성공")
    void t2() {
        // given
        Long userId = 1L;
        List<Notification> notifications = List.of(
            Notification.builder()
                .userId(userId)
                .type(NotificationType.DELAYED_BID_OUTBID)
                .message("입찰이 밀렸습니다.")
                .build(),
            Notification.builder()
                .userId(userId)
                .type(NotificationType.LIVE_AUCTION_START)
                .message("찜한 라이브 경매가 시작되었습니다.")
                .build()
        );

        given(repository.findByUserIdAndCheckFalseOrderByCreateDateDesc(userId))
            .willReturn(notifications);

        // when
        List<NotificationDto> result = notificationService.getUnreadNotifications(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).userId()).isEqualTo(userId);
        assertThat(result.get(1).userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("t3: 읽지 않은 알림 개수 조회 성공")
    void t3() {
        Long userId = 1L;
        Long expectedCount = 5L;

        given(repository.countByUserIdAndCheckFalse(userId)).willReturn(expectedCount);

        //when
        Long result = notificationService.getUnreadCount(userId);

        //then
        assertThat(result).isEqualTo(expectedCount);
    }

    // ========== 알림 읽음 처리 테스트 ==========

    @Test
    @DisplayName("t4: 알림 읽음 처리 성공")
    void t4() {
        // given
        Long notificationId = 1L;
        Long userId = 1L;

        Notification notification = Notification.builder()
            .userId(userId)
            .type(NotificationType.DELAYED_BID_OUTBID)
            .message("낙찰되었습니다!")
            .build();

        given(repository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        notificationService.markAsRead(notificationId, userId);

        // then
        assertThat(notification.isCheck()).isTrue();
    }

    @Test
    @DisplayName("t5: 알림 읽음 처리 실패 - 알림 없음")
    void t5() {
        // given
        Long notificationId = 999L;
        Long userId = 1L;

        given(repository.findById(notificationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, userId))
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
            .userId(ownerId)
            .type(NotificationType.DELAYED_BID_OUTBID)
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
        Long userId = 1L;

        Notification notification = Notification.builder()
            .userId(userId)
            .type(NotificationType.PAYMENT_REMINDER)
            .message("결제가 완료되었습니다.")
            .build();

        given(repository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        notificationService.deleteNotification(notificationId, userId);

        // then
        verify(repository).delete(notification);
    }

    @Test
    @DisplayName("t8: 알림 삭제 실패 - 알림 없음")
    void t8() {
        // given
        Long notificationId = 999L;
        Long userId = 1L;

        given(repository.findById(notificationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.deleteNotification(notificationId, userId))
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
            .userId(ownerId)
            .type(NotificationType.DELAYED_BID_OUTBID)
            .message("낙찰되었습니다!")
            .build();

        given(repository.findById(notificationId)).willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() -> notificationService.deleteNotification(notificationId, otherUserId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_FORBIDDEN);
    }


}
