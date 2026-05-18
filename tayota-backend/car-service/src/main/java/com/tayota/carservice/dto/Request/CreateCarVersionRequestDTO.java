package com.tayota.carservice.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateCarVersionRequestDTO {
    @NotBlank
    @JsonProperty("car_series_id")
    private String carSeriesId;

    @NotBlank
    @Size(max = 50)
    private String version;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @JsonProperty("sale_percent")
    private BigDecimal salePercent;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("image_url")
    private String imageUrl;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("video_url")
    private String videoUrl;

    @Valid
    @NotNull
    private CreateCarSpecificationRequestDTO specification;
}
