package com.tayota.userservice.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class VerificationResponseDTO {
    private boolean success;
    private String message;
    private boolean isVerified;

    // Constructor mặc định cho JSON deserialization
    public VerificationResponseDTO() {
    }

    // Constructor cho trường hợp không có isVerified
    public VerificationResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.isVerified = false;
    }
}

