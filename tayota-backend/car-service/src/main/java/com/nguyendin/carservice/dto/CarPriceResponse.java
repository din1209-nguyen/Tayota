package com.nguyendin.carservice.dto;

import java.math.BigDecimal;

public record CarPriceResponse(
        String exterior,
        String interior,
        BigDecimal price
) {
}
