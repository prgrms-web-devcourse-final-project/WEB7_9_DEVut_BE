package devut.buzzerbidder.global.response;

import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ApiResponse<T> {

    private String resultCode;
    private String msg;
    private T data;

    public ApiResponse(String resultCode, String msg) {
        this.resultCode = resultCode;
        this.msg = msg;
        this.data = null;
    }

    // 성공 응답 생성
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("200", "성공", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>("200", message, data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>("200", message, null);
    }

    // 에러 응답 생성
    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, T data) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), data);
    }

}