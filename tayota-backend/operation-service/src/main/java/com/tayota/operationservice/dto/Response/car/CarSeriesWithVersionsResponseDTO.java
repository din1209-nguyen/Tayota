package com.tayota.operationservice.dto.response.car;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarSeriesWithVersionsResponseDTO {
    private UUID id;
    private String name;
    private String description;
    private List<CarVersionItemResponseDTO> versions;
}
