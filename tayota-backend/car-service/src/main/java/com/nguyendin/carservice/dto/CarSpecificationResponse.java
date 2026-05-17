package com.nguyendin.carservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record CarSpecificationResponse(
        @JsonProperty("car_version_id")
        UUID carVersionId,
        String origin,
        String fuel,
        @JsonProperty("number_of_seats")
        Integer numberOfSeats,
        Integer length,
        Integer width,
        Integer height,
        Integer capacity,
        @JsonProperty("cylinder_capacity")
        String cylinderCapacity,
        Integer cylinder,
        String gearbox,
        @JsonProperty("maximum_speed")
        Integer maximumSpeed,
        String acceleration,
        String torque,
        @JsonProperty("gross_weight_allowance")
        Integer grossWeightAllowance,
        String trademarks
) {
}
