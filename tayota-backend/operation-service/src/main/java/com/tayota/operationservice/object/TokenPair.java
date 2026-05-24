package com.tayota.operationservice.object;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenPair {
    String accessToken;
    String refreshToken;
}
