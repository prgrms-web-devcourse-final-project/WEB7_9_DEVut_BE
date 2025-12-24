package devut.buzzerbidder.domain.notification.listener;

import devut.buzzerbidder.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DelayedBidNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DelayedBidNotificationListener listener;

    // ========== 입찰가 밀림 테스트 ==========


}
