package devut.buzzerbidder.domain.chat.dto.response;

import devut.buzzerbidder.domain.chat.dto.DirectMessageDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;

@Schema(description = "DM 채팅방 조회 응답")
public record DirectMessageEnterResponse(
        @Schema(description = "기존 채팅방 존재 여부")
        boolean exists,

        @Schema(description = "채팅방 ID (웹소켓 구독용, 없으면 null)")
        Long chatRoomId,

        @Schema(description = "상품 정보")
        ItemInfo itemInfo,

        @Schema(description = "메시지 목록 (기존 채팅방이 있을 경우)")
        List<DirectMessageDto> messages
) {
    @Schema(description = "상품 정보")
    public record ItemInfo(
            @Schema(description = "상품 ID")
            Long itemId,

            @Schema(description = "상품명")
            String itemName,

            @Schema(description = "현재 가격")
            Long currentPrice,

            @Schema(description = "상품 이미지 URL")
            String itemImageUrl,

            @Schema(description = "경매 상태")
            String auctionStatus
    ) {}

    // 채팅방이 없을 때
    public static DirectMessageEnterResponse notExists(ItemInfo itemInfo) {
        return new DirectMessageEnterResponse(false, null, itemInfo, Collections.emptyList());
    }

    // 채팅방이 있을 때
    public static DirectMessageEnterResponse exists(Long chatRoomId, ItemInfo itemInfo, List<DirectMessageDto> messages) {
        return new DirectMessageEnterResponse(true, chatRoomId, itemInfo, messages);
    }
}
