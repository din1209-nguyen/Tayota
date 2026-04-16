package com.nguyendin.commoncore.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Class định dạng lại Response thống nhất để trả về cho client
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL) // Các trường null sẽ không được trả về
public class ApiResponse<T> {

    private boolean isSuccess;
    private int code;
    private String message;
    private T result;
    private LocalDateTime timestamp;

    public ApiResponse(boolean isSuccess, int code, String message, T result) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
        this.result = result;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(boolean isSuccess, int code, String message) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Tự xây dựng trạng thái Response
    public static <T> ApiResponse<T> build(boolean isSuccess, int code, String message, T result) {
        return new ApiResponse<>(isSuccess, code, message, result);
    }

    // Xây dựng Response với trạng thái thành công
    public static <T> ApiResponse<T> success(int code, String message, T result) {
        return new ApiResponse<>(true, code, message, result);
    }

    // Xây dựng Response với trạng thái thất bại (khng có body)
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, code, message);
    }
}