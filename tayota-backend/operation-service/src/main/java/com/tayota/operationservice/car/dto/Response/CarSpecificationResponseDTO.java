package com.tayota.operationservice.car.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarSpecificationResponseDTO {
    private UUID carVersionId;
    private String origin;
    private String fuel;
    private Integer numberOfSeats;
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer capacity;
    private String cylinderCapacity;
    private Integer cylinder;
    private String gearbox;
    private Integer maximumSpeed;
    private String acceleration;
    private String torque;
    private Integer grossWeightAllowance;
    private String trademarks;
}
