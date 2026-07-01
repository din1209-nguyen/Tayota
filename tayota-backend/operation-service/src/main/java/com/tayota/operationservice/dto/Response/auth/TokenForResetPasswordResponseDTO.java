package com.tayota.operationservice.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenForResetPasswordResponseDTO {
    String token;
}
