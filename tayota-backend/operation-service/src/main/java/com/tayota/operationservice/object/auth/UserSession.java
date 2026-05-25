package com.tayota.operationservice.object.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    private String refreshHash;
    private String clientIp;
    private String userAgent;
    private Instant loginAt;
}
