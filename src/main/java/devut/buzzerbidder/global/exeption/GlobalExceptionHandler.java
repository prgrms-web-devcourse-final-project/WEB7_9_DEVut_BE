package devut.buzzerbidder.global.exeption;

import devut.buzzerbidder.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 로직 예외 처리 (BusinessException)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        // HTTP 상태 코드에 따라 로깅 레벨 구분
        if (errorCode.getStatus().is5xxServerError()) {
            // 5xx: 서버 오류 - error 레벨 (스택 트레이스 포함)
            log.error("Business exception occurred: code={}, message={}",
                    errorCode.getCode(),
                    errorCode.getMessage(),
                    ex);
        } else {
            // 4xx: 클라이언트 오류 - warn 레벨 (스택 트레이스 제외)
            log.warn("Business exception occurred: code={}, message={}",
                    errorCode.getCode(),
                    errorCode.getMessage());
        }

        ApiResponse<Void> response = ApiResponse.error(errorCode);
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    // 요청 유효성 검사 실패 (400 Bad Request)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        // 필드별로 에러 메시지를 Map으로 수집
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage() != null
                    ? error.getDefaultMessage()
                    : "검증에 실패했습니다";

            // 동일한 필드에 여러 에러가 있을 경우 ", "로 연결
            errors.merge(fieldName, errorMessage, (existing, newMsg) -> existing + ", " + newMsg);
        });

        log.warn("Validation failed: {}", errors);

        ApiResponse<Map<String, String>> response = ApiResponse.error(
                ErrorCode.VALIDATION_FAILED, errors);
        return new ResponseEntity<>(response, ErrorCode.VALIDATION_FAILED.getStatus());
    }

    // 잘못된 요청 형식
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Invalid request format: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ErrorCode.BAD_REQUEST_FORMAT);
        return new ResponseEntity<>(response, ErrorCode.BAD_REQUEST_FORMAT.getStatus());
    }

    // NoSuchElementException 처리
    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoSuchElementException(java.util.NoSuchElementException ex) {
        log.warn("NoSuchElementException: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ErrorCode.NOT_FOUND_DATA);
        return new ResponseEntity<>(response, ErrorCode.NOT_FOUND_DATA.getStatus());
    }

    // SecurityException 처리
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(SecurityException ex) {
        log.warn("SecurityException: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ErrorCode.FORBIDDEN_ACCESS);
        return new ResponseEntity<>(response, ErrorCode.FORBIDDEN_ACCESS.getStatus());
    }

    // 그 외 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected exception occurred", ex);
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INTERNAL_ERROR);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

