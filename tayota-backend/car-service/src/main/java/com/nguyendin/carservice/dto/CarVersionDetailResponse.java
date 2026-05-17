package com.nguyendin.carservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CarVersionDetailResponse(
        UUID id,
        String version,
        CarSeriesResponse series,
        CarSpecificationResponse specification,
        List<CarPriceResponse> prices,
        List<CarGalleryResponse> gallery,
        List<CarArticleResponse> articles,
        @JsonProperty("sale_percent")
        BigDecimal salePercent
) {
}
