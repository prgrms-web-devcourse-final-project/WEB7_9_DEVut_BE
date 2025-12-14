package devut.buzzerbidder.global.notification.dto;

import devut.buzzerbidder.global.notification.enums.AuctionNotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "경매 알림 응답")
public record AuctionNotificationResponse(
        @Schema(description = "알림 타입", example = "NEW_BID")
        AuctionNotificationType type,

        @Schema(description = "알림 메시지", example = "새로운 입찰이 들어왔습니다")
        String message,

        @Schema(description = "현재 입찰가", example = "50000")
        Integer currentBid,

        @Schema(description = "입찰자 닉네임", example = "홍길동")
        String bidderNickname,

        @Schema(description = "발생 시간")
        LocalDateTime timestamp
) {
    public static AuctionNotificationResponse of(
            AuctionNotificationType type,
            String message,
            Integer currentBid,
            String bidderNickname
    ) {
        return new AuctionNotificationResponse(
                type,
                message,
                currentBid,
                bidderNickname,
                LocalDateTime.now()
        );
    }
}