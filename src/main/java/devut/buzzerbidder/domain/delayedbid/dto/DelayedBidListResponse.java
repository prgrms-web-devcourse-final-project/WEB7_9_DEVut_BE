package devut.buzzerbidder.domain.delayedbid.dto;

import java.util.List;

public record DelayedBidListResponse(
    List<DelayedBidResponse> bids,
    long totalCount
) {

}
