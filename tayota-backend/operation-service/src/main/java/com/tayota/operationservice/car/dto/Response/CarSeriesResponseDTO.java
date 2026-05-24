package com.tayota.operationservice.car.dto.Response;

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
