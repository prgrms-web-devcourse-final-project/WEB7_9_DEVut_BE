package devut.buzzerbidder.domain.liveBid.dto.request;

import jakarta.validation.constraints.NotNull;

public record LiveBidRequest(
        @NotNull Long liveItemId,
        @NotNull Long auctionId,
        @NotNull Long sellerId,
        @NotNull int bidPrice
) {}
