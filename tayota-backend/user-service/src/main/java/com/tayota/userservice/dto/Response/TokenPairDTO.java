package com.tayota.userservice.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenPairDTO {
    String accessToken;
    String refreshToken;
}
