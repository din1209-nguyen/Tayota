package com.tayota.carservice.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CarPriceResponseDTO {
    private String exterior;
    private String interior;
    private BigDecimal price;
}
