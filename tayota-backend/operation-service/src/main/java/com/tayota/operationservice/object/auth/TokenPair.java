package com.tayota.operationservice.object.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenPair {
    String accessToken;
    String refreshToken;
}
