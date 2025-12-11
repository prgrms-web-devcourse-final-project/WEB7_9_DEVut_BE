package devut.buzzerbidder.domain.chat.dto.request;

public record AuctionChatMessageRequest(
        Long auctionId,
        String message
) {}
