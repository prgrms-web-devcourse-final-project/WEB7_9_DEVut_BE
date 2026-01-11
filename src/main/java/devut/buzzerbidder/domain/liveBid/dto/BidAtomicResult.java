package devut.buzzerbidder.domain.liveBid.dto;

public record BidAtomicResult(long code, Long balanceBefore, Long balanceAfter, Long refundFailed) {
    public boolean isSuccess() { return code == 1L; }
}