package com.tayota.operationservice.dto.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(404, "Không tìm thấy người dùng!"),
    EMAIL_NOT_NULL(400, "Email không được để trống!"),
    EMAIL_NOT_FOUND(404, "Không tìm thấy email!"),
    EMAIL_ALREADY_EXISTS(409, "Email đã tồn tại!"),
    INVALID_PASSWORD(400, "Mật khẩu không đúng"),
    INVALID_REFRESH_TOKEN(401, "Refresh token không hợp lệ!");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
