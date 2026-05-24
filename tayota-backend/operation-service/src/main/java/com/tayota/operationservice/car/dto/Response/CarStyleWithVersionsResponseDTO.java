package com.tayota.operationservice.car.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarStyleWithVersionsResponseDTO {
    private UUID id;
    private String name;
    private String description;
    private List<CarSeriesWithVersionsResponseDTO> series;
}
