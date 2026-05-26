package com.tayota.operationservice.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdvisorCustomerResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
}
