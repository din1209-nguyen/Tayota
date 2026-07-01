package com.tayota.operationservice.dto.response.admin;

import com.tayota.operationservice.enums.user.RoleType;
import com.tayota.operationservice.enums.user.StatusType;
import com.tayota.operationservice.enums.user.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdminUserResponse {
    private UUID id;
    private String email;
    private RoleType role;
    private StatusType status;
    private ProviderType loginProvider;
    private String fullname;
    private String phone;
    private String avatarUrl;
    private UUID dealershipId;
    private Instant createdAt;
}
