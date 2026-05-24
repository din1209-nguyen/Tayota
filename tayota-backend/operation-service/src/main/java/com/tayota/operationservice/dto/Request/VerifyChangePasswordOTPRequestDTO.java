package com.tayota.operationservice.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class VerifyChangePasswordOTPRequestDTO {
    @NotBlank(message = "Vui lòng nhập mã OTP")
    String otp;
}
