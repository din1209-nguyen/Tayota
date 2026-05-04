package com.tayota.userservice.object;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class UserSession {
    private String refreshHash;
    private String clientIp;
    private String userAgent;
    private Instant loginAt;
}
