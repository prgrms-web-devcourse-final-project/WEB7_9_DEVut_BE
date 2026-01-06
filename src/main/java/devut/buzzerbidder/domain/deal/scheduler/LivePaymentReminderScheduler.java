package devut.buzzerbidder.domain.deal.scheduler;

import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.event.PaymentReminderEvent;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.notification.enums.NotificationType;
import devut.buzzerbidder.domain.notification.repository.NotificationRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LivePaymentReminderScheduler {

    private final LiveDealRepository liveDealRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 라이브 경매 잔금 결제 리마인더 (30분마다 실행)
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 60000) // 30분, 초기 1분 대기
    public void sendPaymentReminders() {
        LocalDateTime now = LocalDateTime.now();

        // PENDING 상태인 거래만 조회
        List<LiveDeal> pendingDeals = liveDealRepository.findAllByStatusWithJoin(DealStatus.PENDING);

        if (pendingDeals.isEmpty()) {
            return;
        }

        for (LiveDeal deal : pendingDeals) {
            try {
                checkAndPublishReminder(deal, now);
            } catch (Exception e) {
                log.error("잔금 리마인더 처리 실패: dealId={}", deal.getId(), e);
            }
        }
    }

    private void checkAndPublishReminder(LiveDeal deal, LocalDateTime now) {
        Duration elapsed = Duration.between(deal.getCreateDate(), now);
        long hours = elapsed.toHours();

        Long buyerId = deal.getBuyer().getId();
        Long dealId = deal.getId();

        // 24시간 경과 (24~25시간 사이) - 24시간 남음
        if (hours >= 24 && hours < 25) {
            if (!isReminderAlreadySent(buyerId, dealId, "24h")) {
                publishReminderEvent(deal, 24);
            }
        }

        // 42시간 경과 (42~43시간 사이) - 6시간 남음
        if (hours >= 42 && hours < 43) {
            if (!isReminderAlreadySent(buyerId, dealId, "6h")) {
                publishReminderEvent(deal, 6);
            }
        }
    }

    private boolean isReminderAlreadySent(Long buyerId, Long dealId, String timeMarker) {
        return notificationRepository.existsPaymentReminder(
            buyerId,
            NotificationType.PAYMENT_REMINDER,
            dealId,
            timeMarker
        );
    }

    private void publishReminderEvent(LiveDeal deal, int remainingHours) {
        eventPublisher.publishEvent(
            new PaymentReminderEvent(
                deal.getId(),
                deal.getBuyer().getId(),
                deal.getItem().getId(),
                deal.getItem().getName(),
                deal.getWinningPrice(),
                remainingHours
            )
        );
    }

}
