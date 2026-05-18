package com.tayota.carservice.dto.Response;

import com.tayota.carservice.enums.CarStatusType;
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
