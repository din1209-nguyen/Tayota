package com.tayota.operationservice.dto.request.car;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.UUID;

@Getter
public class AssignCustomerVehicleRequest {
    @NotNull(message = "Khách hàng không được để trống")
    private UUID userId;

    @NotBlank(message = "Số VIN không được để trống")
    @Size(min = 17, max = 17, message = "Số VIN phải gồm 17 ký tự")
    private String vinId;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;
}
