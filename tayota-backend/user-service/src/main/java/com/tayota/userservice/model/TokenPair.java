package com.tayota.userservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenPair {
    String accessToken;
    String refreshToken;
}
