package com.tayota.carservice.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarVersionDetailResponseDTO {
    private UUID id;
    private String version;
    private CarSeriesResponseDTO series;
    private CarSpecificationResponseDTO specification;
    private List<CarPriceResponseDTO> prices;
    private List<CarGalleryResponseDTO> gallery;
    private List<CarArticleResponseDTO> articles;

    @JsonProperty("sale_percent")
    private BigDecimal salePercent;
}
