package com.tayota.operationservice.dto.response.car;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarVersionItemResponseDTO {
    private UUID id;
    private String name;
    private Integer modelYear;
    private BigDecimal salePercent;
    private UUID carSeriesId;
    private String carSeriesName;
    private UUID carStyleId;
    private String carStyleName;
    private BigDecimal minPrice;
    private String imageUrl;
    private CarSpecificationResponseDTO specification;
}
