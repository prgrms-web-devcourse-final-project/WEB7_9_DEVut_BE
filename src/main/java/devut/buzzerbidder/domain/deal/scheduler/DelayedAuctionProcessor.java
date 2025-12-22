package devut.buzzerbidder.domain.deal.scheduler;

import devut.buzzerbidder.domain.deal.service.DelayedDealService;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem.AuctionStatus;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DelayedAuctionProcessor {

    private final DelayedItemRepository delayedItemRepository;
    private final DelayedDealService delayedDealService;

    // 개별 경매 종료 처리 (별도 트랜잭션)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEndedAuction(Long itemId) {

        // 1. 비관적 락으로 조회 (동시성 제어)
        DelayedItem item = delayedItemRepository.findByIdWithLock(itemId)
            .orElse(null);

        if (item == null) {
            log.warn("경매 아이템이 없음 : itemId={}", itemId);
            return;
        }

        // 2. 상태 재확인 (조회 후 변경되었을 수 있음 - Double Check)
        if (item.getAuctionStatus() != AuctionStatus.BEFORE_BIDDING
            && item.getAuctionStatus() != AuctionStatus.IN_PROGRESS) {
            log.warn("이미 처리된 경매 : itemId={}, status={}", itemId, item.getAuctionStatus());
            return;
        }

        // 3. 종료 시간 재확인
        if (item.getEndTime().isAfter(LocalDateTime.now())) {
            log.warn("종료 시간이 아직 안 됨: itemId={}", itemId);
            return;
        }

        // 4. 입찰 여부 확인 및 상태 변경
        boolean hasBids = delayedDealService.hasBidsForAuction(itemId);

        if (hasBids) {
            // 입찰이 있으면 Deal 생성
            try {
                delayedDealService.createDealFromAuction(itemId);
                item.changeAuctionStatus(AuctionStatus.IN_DEAL);
                log.info("경매 종료 처리 성공 (낙찰): itemId={}", itemId);
            } catch (BusinessException e) {
                log.error("경매 종료 처리 중 비즈니스 예외: itemId={}, error={}", itemId, e.getErrorCode(), e);
                throw e;
            }
        } else {
            // 입찰이 없으면 유찰 처리
            item.changeAuctionStatus(AuctionStatus.FAILED);
            log.info("경매 종료 처리 성공 (유찰): itemId={}", itemId);
        }
    }

}
