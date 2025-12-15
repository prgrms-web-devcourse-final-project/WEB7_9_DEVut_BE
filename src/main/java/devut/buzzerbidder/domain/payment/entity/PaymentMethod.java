package devut.buzzerbidder.domain.payment.entity;

import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;

public enum PaymentMethod {
    CARD,
    EASY_PAY,
    VIRTUAL_ACCOUNT,
    MOBILE_PHONE,
    TRANSFER,
    CULTURE_GIFT_CERTIFICATE,
    BOOK_GIFT_CERTIFICATE,
    GAME_GIFT_CERTIFICATE;

    public static PaymentMethod fromToss(String tossValue) {
        return switch (tossValue) {
            case "간편결제" -> EASY_PAY;
            case "카드" -> CARD;
            case "계좌이체" -> TRANSFER;
            case "가상계좌" -> VIRTUAL_ACCOUNT;
            case "휴대폰" -> MOBILE_PHONE;
            case "문화상품권" -> CULTURE_GIFT_CERTIFICATE;
            case "도서문화상품권" -> BOOK_GIFT_CERTIFICATE;
            case "게임문화상품권" -> GAME_GIFT_CERTIFICATE;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST_FORMAT);
        };
    }
}
