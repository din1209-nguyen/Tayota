package com.tayota.operationservice.exception;

import com.tayota.operationservice.dto.common.ErrorCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final int code;

    public CustomException(int code, String message) {
        super(message);
        this.code = code;
    }

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
