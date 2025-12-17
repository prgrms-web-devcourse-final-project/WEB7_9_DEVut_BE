package devut.buzzerbidder.global.exeption;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ========== 공통 에러 ==========
    VALIDATION_FAILED("CMN001", HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다."),
    INTERNAL_ERROR("CMN002", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE("CMN003", HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INVALID_TYPE_VALUE("CMN004", HttpStatus.BAD_REQUEST, "잘못된 타입의 값입니다."),
    MISSING_REQUEST_PARAMETER("CMN005", HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
    UNAUTHORIZED_ACCESS("CMN006", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN_ACCESS("CMN007", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND_DATA("CMN008", HttpStatus.NOT_FOUND, "존재하지 않는 데이터입니다."),
    BAD_REQUEST_FORMAT("CMN009", HttpStatus.BAD_REQUEST, "잘못된 형식의 요청 데이터입니다."),

    // ========== User 도메인 에러 ==========
    USER_NOT_FOUND("M001", HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    USER_EMAIL_DUPLICATE("M002", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    USER_NICKNAME_DUPLICATE("M003", HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    USER_PASSWORD_MISMATCH("M004", HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다."),
    USER_LOGIN_FAILED("M005", HttpStatus.UNAUTHORIZED, "로그인에 실패했습니다."),
    USER_TOKEN_INVALID("M006", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    USER_TOKEN_EXPIRED("M007", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    USER_PROVIDER_ID_DUPLICATE("M008", HttpStatus.CONFLICT, "이미 사용 중인 SNS ID입니다."),
    USER_SOCIAL_ACCOUNT("M009", HttpStatus.BAD_REQUEST, "소셜 로그인으로 가입한 계정입니다. 소셜 로그인을 사용해주세요."),
    USER_INVALID_PROVIDER("M010", HttpStatus.BAD_REQUEST, "유효하지 않은 로그인 제공자입니다."),
    USER_EMAIL_NOT_VERIFIED("M011", HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),

    // ========== Deal 도메인 에러 ==========
    DEAL_NOT_FOUND("D001", HttpStatus.NOT_FOUND, "존재하지 않는 거래입니다."),
    DEAL_INVALID_TYPE("D002", HttpStatus.BAD_REQUEST, "잘못된 경매 유형입니다."),
    DEAL_DELIVERY_INFO_NOT_FOUND("D003", HttpStatus.NOT_FOUND, "배송 정보가 존재하지 않습니다."),

    // ========== Wallet 도메인 에러 ==========
    WALLET_NOT_FOUND("W001", HttpStatus.NOT_FOUND, "지갑이 존재하지 않습니다."),
    BIZZ_INSUFFICIENT_BALANCE("W002", HttpStatus.BAD_REQUEST, "BIZZ 잔액이 부족합니다."),
    INVALID_WALLET_TRANSACTION_TYPE("W004", HttpStatus.INTERNAL_SERVER_ERROR, "잘못된 거래 유형입니다."),
    INVALID_WALLET_AMOUNT("W005", HttpStatus.BAD_REQUEST, "충전/차감 금액은 null 또는 0 이하일 수 없습니다."),
    INVALID_TRANSFER("W006", HttpStatus.BAD_REQUEST, "자기 자신에게 지불할 수 없습니다."),
    WALLET_ALREADY_EXISTS("W007", HttpStatus.CONFLICT, "사용자 지갑이 이미 존재합니다."),
    // ========== Payment 도메인 에러 ==========
    PAYMENT_NOT_FOUND("P001", HttpStatus.NOT_FOUND, "결제정보를 찾을 수 없습니다."),
    NOT_PENDING_PAYMENT("P002", HttpStatus.CONFLICT, "결제대기 상태가 아닙니다."),
    INVALID_AMOUNT("P003", HttpStatus.BAD_REQUEST, "결제금액이 올바르지 않습니다."),
    PAYMENT_CONFIRM_FAILED("P004", HttpStatus.BAD_REQUEST, "결제승인 요청이 실패했습니다."),
    INVALID_DATE_RANGE("P005", HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다."),
    NOT_SUCCESS_PAYMENT("P006", HttpStatus.CONFLICT, "결제완료 상태가 아닙니다."),
    AMOUNT_EXCEEDS_LIMIT("P007", HttpStatus.BAD_REQUEST, "취소 가능 금액을 초과했습니다."),
    PAYMENT_CANCELED_FAILED("P008", HttpStatus.BAD_REQUEST, "결제취소 요청이 실패했습니다."),
    // ========== AuctionRoom 도메인 에러 ==========
    FULL_AUCTION_ROOM("AR001", HttpStatus.CONFLICT, "경매방이 가득 찼습니다."),
    AUCTION_ROOM_BUSY("AR002", HttpStatus.CONFLICT, "잠시 후 다시 시도해주세요."),
    // ========== LIVEITEM 도메인 에러 ==========
    CLOSE_LIVETIME("LI001", HttpStatus.BAD_REQUEST,"경매시작 시간은 최소 1시간 이후여야합니다."),
    INVALID_LIVETIME("LI002", HttpStatus.BAD_REQUEST,"유효한 경매 시작 시간이 아닙니다."),
    EDIT_UNAVAILABLE("LI003", HttpStatus.BAD_REQUEST,"1시간안에 시작하는 경매는 수정 및 삭제가 불가능합니다."),
    IMAGE_FILE_EMPTY("LI003", HttpStatus.BAD_REQUEST,"이미지는 필수입니다."),

    // ========== Notification 도메인 에러 ==========
    NOTIFICATION_NOT_FOUND("NO001", HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    NOTIFICATION_FORBIDDEN("NO002", HttpStatus.FORBIDDEN, "알림에 접근할 권한이 없습니다."),

    // ========== Image 도메인 에러 ==========
    IMAGE_INVALID_FILE_TYPE("IMG001", HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    IMAGE_FILE_TOO_LARGE("IMG002", HttpStatus.BAD_REQUEST, "파일 크기가 너무 큽니다. (최대 10MB)"),
    IMAGE_INVALID_DIRECTORY("IMG003", HttpStatus.BAD_REQUEST, "허용되지 않은 디렉토리입니다."),
    IMAGE_URL_INVALID("IMG004", HttpStatus.BAD_REQUEST, "유효하지 않은 이미지 URL입니다."),
    IMAGE_URL_NOT_ALLOWED("IMG005", HttpStatus.FORBIDDEN, "허용되지 않은 S3 버킷입니다."),
    IMAGE_UPLOAD_FAILED("IMG006", HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다."),

    // ========== chat 도메인 에러 ==========
    CHATROOM_NOT_FOUND("C001", HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_NOT_PARTICIPANT("C002", HttpStatus.NOT_FOUND, "참여중이지 않은 채팅방입니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

}