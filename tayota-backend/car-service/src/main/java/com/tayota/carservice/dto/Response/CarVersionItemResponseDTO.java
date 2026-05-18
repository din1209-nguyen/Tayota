package com.tayota.carservice.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CarVersionItemResponseDTO implements Serializable {
    private UUID id;
    private String version;
    private String series;
    private String style;

    @JsonProperty("min_price")
    private BigDecimal minPrice;

    @JsonProperty("sale_percent")
    private BigDecimal salePercent;

    @JsonProperty("image_url")
    private String imageUrl;
}
