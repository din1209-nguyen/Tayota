package com.tayota.operationservice.dto.response.car;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarVersionDetailResponseDTO {
    private UUID id;
    private String name;
    private Integer modelYear;
    private BigDecimal salePercent;
    private String videoUrl;
    private CarSeriesResponseDTO carSeries;
    private CarSpecificationResponseDTO specification;
    private List<CarPriceResponseDTO> prices;
    private List<CarGalleryResponseDTO> galleries;
    private List<CarArticleResponseDTO> articles;
    private List<AccessoryResponseDTO> accessories;
    private boolean visible;
}
