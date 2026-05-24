package com.tayota.operationservice.dto.response.car;

import com.tayota.operationservice.enums.car.CarStatusType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CarResponseDTO {
    private String vinId;
    private UUID carVersionId;
    private String carVersionName;
    private UUID dealershipId;
    private String dealershipName;
    private String engineNumber;
    private UUID ownerUserId;
    private CarStatusType status;
    private Instant productedYear;
}
