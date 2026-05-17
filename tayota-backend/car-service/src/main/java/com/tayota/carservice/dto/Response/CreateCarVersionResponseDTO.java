package com.tayota.carservice.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CreateCarVersionResponseDTO {
    private UUID id;
    private String message;
}
