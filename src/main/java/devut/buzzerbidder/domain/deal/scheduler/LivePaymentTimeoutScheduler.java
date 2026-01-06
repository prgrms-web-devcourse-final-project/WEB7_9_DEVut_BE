package devut.buzzerbidder.domain.deal.scheduler;

import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import devut.buzzerbidder.domain.deal.service.LiveDealService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LivePaymentTimeoutScheduler {

    private final LiveDealRepository liveDealRepository;
    private final LiveDealService liveDealService;

    private static final long PAYMENT_DEADLINE_HOURS = 48;

    /**
     * 결제 기한 초과 거래 자동 취소 (10분마다 실행)
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 120000) // 10분, 초기 2분 대기
    public void processPaymentTimeouts() {
        LocalDateTime now = LocalDateTime.now();

        // PENDING 상태 거래 조회
        List<LiveDeal> pendingDeals = liveDealRepository.findByStatusWithJoin(DealStatus.PENDING);

        if (pendingDeals.isEmpty()) {
            return;
        }

        int cancelledCount = 0;

        for (LiveDeal deal : pendingDeals) {
            try {
                Duration elapsed = Duration.between(deal.getCreateDate(), now);
                long hours = elapsed.toHours();

                // 48시 초과 체크
                if (hours >= PAYMENT_DEADLINE_HOURS) {
                    liveDealService.cancelDueToPaymentTimeout(deal.getId());
                }
            } catch (Exception e) {
                log.error("결제 타임아웃 처리 실패: dealId = {}", deal.getId(), e);
            }
        }
    }
}
