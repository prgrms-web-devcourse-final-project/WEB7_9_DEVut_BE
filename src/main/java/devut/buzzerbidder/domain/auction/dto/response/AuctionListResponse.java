package devut.buzzerbidder.domain.auction.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AuctionListResponse(
    List<AuctionItem> auctions,
    long totalCount
) {
    public record AuctionItem(
        Long id,
        String auctionType,
        String itemName,
        String itemImageUrl,
        Long currentPrice,
        LocalDateTime endTime,
        String auctionStatus,
        Long roomId,
        Boolean isLiked
    ) {}

}
