package com.tayota.carservice.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarSpecificationResponseDTO {
    @JsonProperty("car_version_id")
    private UUID carVersionId;

    private String origin;
    private String fuel;

    @JsonProperty("number_of_seats")
    private Integer numberOfSeats;

    private Integer length;
    private Integer width;
    private Integer height;
    private Integer capacity;

    @JsonProperty("cylinder_capacity")
    private String cylinderCapacity;

    private Integer cylinder;
    private String gearbox;

    @JsonProperty("maximum_speed")
    private Integer maximumSpeed;

    private String acceleration;
    private String torque;

    @JsonProperty("gross_weight_allowance")
    private Integer grossWeightAllowance;

    private String trademarks;
}
