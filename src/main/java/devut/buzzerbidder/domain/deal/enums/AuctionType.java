package devut.buzzerbidder.domain.deal.enums;

import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;

public enum AuctionType {
    LIVE,
    DELAYED;

    public static AuctionType fromString(String value) {
        for (AuctionType t : values()) {
            if (t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new BusinessException(ErrorCode.DEAL_INVALID_TYPE);
    }
}
