package com.tayota.carservice.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarSeriesResponseDTO {
    private UUID id;
    private String name;
    private String description;
    private CarStyleResponseDTO style;
}
