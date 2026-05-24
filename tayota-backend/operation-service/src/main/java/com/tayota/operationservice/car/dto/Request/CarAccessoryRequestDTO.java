package com.tayota.operationservice.car.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CarAccessoryRequestDTO {
    @NotBlank(message = "Phiên bản xe không được để trống")
    private String carVersionId;

    @NotBlank(message = "Phụ kiện không được để trống")
    private String accessoryId;
}
