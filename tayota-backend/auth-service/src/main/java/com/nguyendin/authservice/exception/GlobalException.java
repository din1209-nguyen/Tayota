package com.nguyendin.authservice.exception;

import com.nguyendin.commoncore.dto.ApiResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalException {

    // Xử lý các lỗi Validation
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException exception) {
        return ApiResponse.error(
                exception.getStatusCode().value(),
                exception.getMessage()
        );
    }
}
