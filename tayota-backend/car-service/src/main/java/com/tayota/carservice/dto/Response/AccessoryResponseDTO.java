package com.tayota.carservice.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AccessoryResponseDTO {
    private UUID id;
    private String model;
    private String brand;
    private BigDecimal price;
    private String description;
    private String useContent;
    private String reminderContent;
    private String type;
}
