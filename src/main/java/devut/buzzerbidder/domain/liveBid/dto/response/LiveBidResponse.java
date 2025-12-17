package devut.buzzerbidder.domain.liveBid.dto.response;

public record LiveBidResponse(
        boolean isSuccess,
        String message,
        int bidPrice
) {
    public LiveBidResponse(boolean isSuccess, String message, int bidPrice) {
        this.isSuccess = isSuccess;
        this.message = message;
        this.bidPrice = bidPrice;
    }
}
