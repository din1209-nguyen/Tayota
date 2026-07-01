package com.tayota.operationservice.dto.response.car;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class DealershipResponseDTO {
    private UUID id;
    private String name;
    private String address;
    private String phone;
    private String operatingHours;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String placeId;
    private boolean active;
}
