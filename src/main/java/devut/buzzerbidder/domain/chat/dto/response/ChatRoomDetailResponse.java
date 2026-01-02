package devut.buzzerbidder.domain.chat.dto.response;

import devut.buzzerbidder.domain.chat.dto.DirectMessageDto;

import java.util.List;

public record ChatRoomDetailResponse(
        ItemInfo itemInfo,
        // 메시지 목록
        List<DirectMessageDto> messages
) {
    public record ItemInfo(
            Long itemId,
            String itemName,
            Long currentPrice,
            String itemImageUrl,
            String auctionStatus // BEFORE_BIDDING, IN_PROGRESS, ENDED 등
    ) {}
}
