package com.tayota.operationservice.exception;

import com.tayota.operationservice.dto.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@ConditionalOnProperty(prefix = "common.exception", name = "handler-enabled", havingValue = "true", matchIfMissing = true)
public class GlobalExceptionHandler {

    // Xử lý lỗi khi gửi Request thiếu Body hoặc Body không đúng định dạng JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return ApiResponse.error(400, "Dữ liệu yêu cầu không được để trống hoặc sai định dạng JSON");
    }

    // Xử lý các lỗi Validation
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ApiResponse<Map<String, String>> handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();

        // Lấy ra kết quả ghi nhận lỗi (getBindingResult) và lấy ra các trường lỗi (getFieldErrors)
        // Duyệt qua các phần tử trong trường lỗi để format về dạng key-value
        exception.getBindingResult().getFieldErrors().forEach(error ->
                // Thêm vào danh sách lỗi để trả về cho client
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ApiResponse.error(400, "Dữ liệu không hợp lệ", errors);
    }

    // Xử lý các lỗi ràng buộc dữ liệu cho các tham số đơn lẻ (@RequestParam, @PathVariable) trong Controller
    // Áp dụng cho các tham số rời rạc không nằm trong DTO (như String, Long...)
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Map<String, String>> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> errors = new HashMap<>();

        // Duyệt qua từng lỗi vi phạm ràng buộc để lấy tên tham số và thông báo lỗi
        exception.getConstraintViolations().forEach(violation -> {
            // Lấy tên tham số (ví dụ: token, email)
            String paramName = ((PathImpl) violation.getPropertyPath()).getLeafNode().getName();
            // Lấy thông báo lỗi
            String message = violation.getMessage();
            // Thêm vào danh sách lỗi để trả về cho client
            errors.put(paramName, message);
        });
        return ApiResponse.error(400, "Dữ liệu không hợp lệ", errors);
    }

    // Xử lý lỗi phân quyền (Khi @PreAuthorize thất bại)
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException exception) {
        return ApiResponse.error(403, "Bạn không có quyền thực hiện chức năng này!");
    }

    // Xử lý lỗi DaoAuthenticationProvider khi hàm loadUserByUsername() của văng ra bất kỳ một RuntimeException nào không phải là lỗi xác thực chuẩn của Spring
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ApiResponse<Void> handleInternalAuthException(InternalAuthenticationServiceException exception) {
        // Lấy nguyên nhân chính
        Throwable cause = exception.getCause();

        // Nếu là CustomException thì ép kiểu và lấy đúng Code
        if (cause instanceof CustomException customException) {
            return ApiResponse.error(customException.getCode(), customException.getMessage());
        }

        // Nếu không phải, trả về lỗi 500 mặc định
        return ApiResponse.error(500, "Lỗi xác thực hệ thống: " + exception.getMessage());
    }

    // Xử lý các lỗi CustomException tự định nghĩa trong ứng dụng
    @ExceptionHandler(value = CustomException.class)
    public ApiResponse<Void> handleCustomException(CustomException exception) {
        return ApiResponse.error(exception.getCode(), exception.getMessage());
    }

    // Xử lý các lỗi không được dự đoán trước
    @ExceptionHandler(value = Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.error(exception.hashCode(), "Lỗi hệ thống: " + exception.getMessage());
    }
}