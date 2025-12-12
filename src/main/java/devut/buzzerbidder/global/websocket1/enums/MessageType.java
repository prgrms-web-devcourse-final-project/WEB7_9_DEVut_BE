package devut.buzzerbidder.global.websocket1.enums;

public enum MessageType {
    // 경매 관련
    AUCTION_START,
    AUCTION_BID,
    AUCTION_END,

    // 채팅 관련
    CHAT_MESSAGE,

    // 연결 관련
    USER_JOINED,
    USER_LEFT,

    // 에러
    ERROR
}
