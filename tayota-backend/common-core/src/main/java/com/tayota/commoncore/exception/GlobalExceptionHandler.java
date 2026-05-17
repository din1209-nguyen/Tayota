package com.tayota.commoncore.exception;

import com.tayota.commoncore.dto.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@ConditionalOnProperty(prefix = "common.exception", name = "handler-enabled", havingValue = "true", matchIfMissing = true)
public class GlobalExceptionHandler {
    // Xử lý lỗi validation
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ApiResponse<Map<String, String>> handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();

        // Lấy danh sách field lỗi và format về dạng key-value
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ApiResponse.error(
                400,
                "Validation error",
                errors
        );
    }

    // Xử lý lỗi nghiệp vụ do service chủ động ném ra
    @ExceptionHandler(value = CustomException.class)
    public ApiResponse<Void> handleCustomException(CustomException exception) {
        return ApiResponse.error(
                exception.getCode(),
                exception.getMessage()
        );
    }

    // Xử lý lỗi không có quyền truy cập
    @ExceptionHandler(value = AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDeniedException() {
        return ApiResponse.error(
                403,
                "Không có quyền truy cập"
        );
    }

    // Xử lý các lỗi không được dự đoán trước
    @ExceptionHandler(value = Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.error(
                exception.hashCode(),
                "Internal server error: " + exception.getMessage()
        );
    }
}
