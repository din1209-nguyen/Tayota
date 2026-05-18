package com.tayota.carservice.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarPriceResponseDTO {
    private UUID exteriorColorId;
    private String exteriorColorName;
    private UUID interiorColorId;
    private String interiorColorName;
    private BigDecimal price;
    private String exImageUrl;
    private String inImageUrl;
}
