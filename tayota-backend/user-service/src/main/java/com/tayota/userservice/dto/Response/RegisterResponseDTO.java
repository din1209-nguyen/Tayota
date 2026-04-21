package com.tayota.userservice.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class RegisterResponseDTO {
    private UUID id;
    private String email;
    private String status;
    private String message;
}

