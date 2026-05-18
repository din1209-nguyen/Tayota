package com.tayota.carservice.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCarSpecificationRequestDTO {
    @NotBlank
    @Size(max = 100)
    private String origin;

    @NotBlank
    @Size(max = 50)
    private String fuel;

    @NotNull
    @JsonProperty("number_of_seats")
    private Integer numberOfSeats;

    @NotNull
    private Integer length;

    @NotNull
    private Integer width;

    @NotNull
    private Integer height;

    private Integer capacity;

    @Size(max = 50)
    @JsonProperty("cylinder_capacity")
    private String cylinderCapacity;

    private Integer cylinder;

    @Size(max = 50)
    private String gearbox;

    @JsonProperty("maximum_speed")
    private Integer maximumSpeed;

    @Size(max = 50)
    private String acceleration;

    @Size(max = 100)
    private String torque;

    @JsonProperty("gross_weight_allowance")
    private Integer grossWeightAllowance;

    @Size(max = 100)
    private String trademarks;
}
