package com.nguyendin.carservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateCarVersionRequest(
        @NotBlank
        @JsonProperty("car_series_id")
        String carSeriesId,

        @NotBlank
        @Size(max = 50)
        String version,

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        @JsonProperty("sale_percent")
        BigDecimal salePercent,

        @NotBlank
        @Size(max = 255)
        @JsonProperty("image_url")
        String imageUrl,

        @NotBlank
        @Size(max = 255)
        @JsonProperty("video_url")
        String videoUrl,

        @Valid
        @NotNull
        CreateCarSpecificationRequest specification
) {
}
