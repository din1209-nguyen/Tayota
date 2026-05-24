package com.tayota.operationservice.dto.response.user;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
public class BasicInformationResponseDTO {
    private String fullname;
    private String avatarUrl;
}