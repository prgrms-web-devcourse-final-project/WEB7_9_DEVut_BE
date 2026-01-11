package devut.buzzerbidder.domain.liveitem.dto.response;

public record AuctionEndMessage(
    String type,
    Long liveItemId,
    String liveItemName,
    String result,
    Long winnerId,
    Integer finalPrice,
    String winnerNickname
) {
    public AuctionEndMessage(Long liveItemId, String liveItemName, String result, Long winnerId, Integer finalPrice, String winnerNickname) {
        this("AUCTION_END", liveItemId, liveItemName, result, winnerId, finalPrice, winnerNickname);
    }
}
