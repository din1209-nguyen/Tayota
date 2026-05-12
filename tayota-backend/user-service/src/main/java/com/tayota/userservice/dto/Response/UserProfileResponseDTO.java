package com.tayota.userservice.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class UserProfileResponseDTO {
    private UUID id;
    private String fullname;
    private String phone;
    private Boolean gender;
    private LocalDate birthDate;
    private String address;
    private String avatarUrl;
    private String email;
    private Instant createdAt;
}
