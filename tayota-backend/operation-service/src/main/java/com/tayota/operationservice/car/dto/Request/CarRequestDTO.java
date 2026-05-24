package com.tayota.operationservice.car.dto.Request;

import com.tayota.operationservice.car.enums.CarStatusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.Instant;

@Getter
public class CarRequestDTO {
    @NotBlank(message = "Số VIN không được để trống")
    @Size(min = 17, max = 17, message = "Số VIN phải có 17 ký tự")
    private String vinId;

    @NotBlank(message = "Phiên bản xe không được để trống")
    private String carVersionId;

    @NotBlank(message = "Đại lý không được để trống")
    private String dealershipId;

    @NotBlank(message = "Số máy không được để trống")
    @Size(max = 50, message = "Số máy không được vượt quá 50 ký tự")
    private String engineNumber;

    private String ownerUserId;

    @NotNull(message = "Trạng thái xe không được để trống")
    private CarStatusType status;

    @NotNull(message = "Năm sản xuất không được để trống")
    private Instant productedYear;
}
