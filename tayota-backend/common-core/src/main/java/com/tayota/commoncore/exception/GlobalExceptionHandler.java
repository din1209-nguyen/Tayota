package com.tayota.commoncore.exception;

import com.tayota.commoncore.dto.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@ConditionalOnProperty(prefix = "common.exception", name = "handler-enabled", havingValue = "true", matchIfMissing = true)
public class GlobalExceptionHandler {
    // Xử lý các lỗi Validation
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ApiResponse<Map<String, String>> handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();

        // Lấy ra kết quả ghi nhận lỗi (getBindingResult) và lấy ra các trường lỗi (getFieldErrors)
        // Duyệt qua các phần tử trong trường lỗi để format về dạng key-value
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        // Trả về Reponse với lỗi
        return ApiResponse.error(
                400,
                "Validation error",
                errors
        );
    }

    @ExceptionHandler(value = CustomException.class)
    public ApiResponse<Void> handleCustomException(CustomException exception) {
        // Trả về Reponse với lỗi
        return ApiResponse.error(
                exception.getCode(),
                exception.getMessage()
        );
    }
}
