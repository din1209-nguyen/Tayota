package com.tayota.operationservice.dto.response.car;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarSeriesResponseDTO {
    private UUID id;
    private UUID carStyleId;
    private String carStyleName;
    private String name;
    private String description;
}
