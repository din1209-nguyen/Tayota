package com.tayota.commoncore.dto;

import lombok.Getter;

@Getter
public enum ErrorCode {

    EMAIL_NOT_FOUND(404, "Không tìm thấy email!"),
    EMAIL_ALREADY_EXISTS(409, "Email đã tồn tại!"),
    INVALID_PASSWORD(400, "Mật khẩu không đúng");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
