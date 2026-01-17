package devut.buzzerbidder.domain.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import devut.buzzerbidder.domain.liveitem.event.LiveAuctionEndedEvent;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.service.NotificationService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LiveAuctionEndedNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LiveAuctionEndedNotificationListener listener;

    // ========== 낙찰 성공 알림 테스트 ==========

    @Test
    @DisplayName("t1: 낙찰 성공시 판매자에게 낙찰 알림 전송")
    void t1() {
        // given
        Long liveItemId = 1L;
        Long sellerUserId = 10L;
        Long winnerUserId = 20L;
        String liveItemName = "테스트 상품";
        Integer finalPrice = 50000;

        LiveAuctionEndedEvent event = new LiveAuctionEndedEvent(
            liveItemId,
            liveItemName,
            sellerUserId,
            true,
            winnerUserId,
            finalPrice
        );

        // when
        listener.handle(event);

        // then
        verify(notificationService).createAndSend(
            eq(sellerUserId),
            eq(NotificationType.LIVE_SUCCESS_SELLER),
            contains("낙찰되었습니다"),
            eq("LIVE_ITEM"),
            eq(liveItemId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t2: 낙찰 성공시 낙찰자에게 낙찰 축하 알림 전송")
    void t2() {
        // given
        Long liveItemId = 2L;
        Long sellerUserId = 15L;
        Long winnerUserId = 25L;
        String liveItemName = "경매 상품";
        Integer finalPrice = 75000;

        LiveAuctionEndedEvent event = new LiveAuctionEndedEvent(
            liveItemId,
            liveItemName,
            sellerUserId,
            true,
            winnerUserId,
            finalPrice
        );

        // when
        listener.handle(event);

        // then
        verify(notificationService).createAndSend(
            eq(winnerUserId),
            eq(NotificationType.LIVE_SUCCESS_BIDDER),
            contains("축하합니다"),
            eq("LIVE_ITEM"),
            eq(liveItemId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t3: 낙찰 성공시 판매자와 낙찰자 모두에게 알림 전송")
    void t3() {
        // given
        Long liveItemId = 3L;
        Long sellerUserId = 30L;
        Long winnerUserId = 40L;
        String liveItemName = "인기 상품";
        Integer finalPrice = 100000;

        LiveAuctionEndedEvent event = new LiveAuctionEndedEvent(
            liveItemId,
            liveItemName,
            sellerUserId,
            true,
            winnerUserId,
            finalPrice
        );

        // when
        listener.handle(event);

        // then
        verify(notificationService, times(2)).createAndSend(
            any(Long.class),
            any(NotificationType.class),
            any(String.class),
            eq("LIVE_ITEM"),
            eq(liveItemId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t4: 판매자 알림 메시지에 상품명과 낙찰가가 포함됨")
    void t4() {
        // given
        String liveItemName = "특별한 상품";
        Integer finalPrice = 120000;

        LiveAuctionEndedEvent event = new LiveAuctionEndedEvent(
            1L,
            liveItemName,
            10L,
            true,
            20L,
            finalPrice
        );

        // when
        listener.handle(event);

        // then
        verify(notificationService).createAndSend(
            eq(10L),
            eq(NotificationType.LIVE_SUCCESS_SELLER),
            contains(liveItemName),
            eq("LIVE_ITEM"),
            eq(1L),
            any(Map.class)
        );

        verify(notificationService).createAndSend(
            eq(10L),
            eq(NotificationType.LIVE_SUCCESS_SELLER),
            contains(String.valueOf(finalPrice)),
            eq("LIVE_ITEM"),
            eq(1L),
            any(Map.class)
        );
    }

    // ========== 유찰 알림 테스트 ==========

    @Test
    @DisplayName("t5: 유찰시 판매자에게 유찰 알림 전송")
    void t5() {
        // given
        Long liveItemId = 5L;
        Long sellerUserId = 50L;
        String liveItemName = "유찰 상품";

        LiveAuctionEndedEvent event = new LiveAuctionEndedEvent(
            liveItemId,
            liveItemName,
            sellerUserId,
            false,
            null,
            null
        );

        // when
        listener.handle(event);

        // then
        verify(notificationService).createAndSend(
            eq(sellerUserId),
            eq(NotificationType.LIVE_FAILED_SELLER),
            contains("유찰되었습니다"),
            eq("LIVE_ITEM"),
            eq(liveItemId),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t6: 유찰시 낙찰자에게는 알림을 전송하지 않음")
    void t6() {
        // given
        Long liveItemId = 6L;
        Long sellerUserId = 60L;
        String liveItemName = "미낙찰 상품";

        LiveAuctionEndedEvent event = new LiveAuctionEndedEvent(
            liveItemId,
            liveItemName,
            sellerUserId,
            false,
            null,
            null
        );

        // when
        listener.handle(event);

        // then
        verify(notificationService, times(1)).createAndSend(
            any(Long.class),
            any(NotificationType.class),
            any(String.class),
            any(String.class),
            any(Long.class),
            any(Map.class)
        );
    }

    @Test
    @DisplayName("t7: 유찰 알림 메시지에 상품명이 포함됨")
    void t7() {
        String liveItemName = "유찰된 상품";

        LiveAuctionEndedEvent event = new LiveAuctionEndedEvent(
            1L,
            liveItemName,
            10L,
            false,
            null,
            null
        );

        // when
        listener.handle(event);

        // then
        verify(notificationService).createAndSend(
            eq(10L),
            eq(NotificationType.LIVE_FAILED_SELLER),
            contains(liveItemName),
            eq("LIVE_ITEM"),
            eq(1L),
            any(Map.class)
        );
    }
}