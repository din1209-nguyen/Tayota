package com.tayota.userservice.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequestDTO {
    @NotBlank(message = "Google id token không được để trống")
    private String idToken;
}
