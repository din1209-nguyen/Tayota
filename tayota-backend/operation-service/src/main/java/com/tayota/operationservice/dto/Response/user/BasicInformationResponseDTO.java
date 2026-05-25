package com.tayota.operationservice.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class BasicInformationResponseDTO {
    private UUID id;
    private String fullname;
    private String avatarUrl;
    private String email;
    private String role;
}
