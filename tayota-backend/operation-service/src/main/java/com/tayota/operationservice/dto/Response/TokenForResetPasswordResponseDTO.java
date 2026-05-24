package com.tayota.operationservice.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenForResetPasswordResponseDTO {
    String token;
}
