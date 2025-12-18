package devut.buzzerbidder.domain.delayedbid.dto;

import java.time.LocalDateTime;

public record DelayedBidResponse(
    Long id,
    Long delayedItemId,
    String bidderNickname,
    Long bidPrice,
    LocalDateTime createdAt
) {

}
