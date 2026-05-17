package com.nguyendin.carservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCarSpecificationRequest(
        @NotBlank
        @Size(max = 100)
        String origin,

        @NotBlank
        @Size(max = 50)
        String fuel,

        @NotNull
        @JsonProperty("number_of_seats")
        Integer numberOfSeats,

        @NotNull
        Integer length,

        @NotNull
        Integer width,

        @NotNull
        Integer height,

        Integer capacity,

        @Size(max = 50)
        @JsonProperty("cylinder_capacity")
        String cylinderCapacity,

        Integer cylinder,

        @Size(max = 50)
        String gearbox,

        @JsonProperty("maximum_speed")
        Integer maximumSpeed,

        @Size(max = 50)
        String acceleration,

        @Size(max = 100)
        String torque,

        @JsonProperty("gross_weight_allowance")
        Integer grossWeightAllowance,

        @Size(max = 100)
        String trademarks
) {
}
