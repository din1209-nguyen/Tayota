package com.tayota.operationservice.dto.response.car;

import com.tayota.operationservice.enums.car.CarStatusType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CustomerVehicleResponse {
    private String vinId;
    private UUID userId;
    private String customerFullName;
    private String customerEmail;
    private String customerPhone;
    private UUID carVersionId;
    private String carVersionName;
    private UUID dealershipId;
    private CarStatusType status;
    private Instant assignedAt;
}
