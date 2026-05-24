package com.tayota.operationservice.car.dto.Request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class CarVersionRequestDTO {
    @NotBlank(message = "Dòng xe không được để trống")
    private String carSeriesId;

    @NotBlank(message = "Tên phiên bản không được để trống")
    @Size(max = 50, message = "Tên phiên bản không được vượt quá 50 ký tự")
    private String name;

    @DecimalMin(value = "0.00", message = "Phần trăm giảm giá không hợp lệ")
    private BigDecimal salePercent = BigDecimal.ZERO;

    @NotNull(message = "Năm mẫu xe không được để trống")
    @Min(value = 1900, message = "Năm mẫu xe không hợp lệ")
    private Integer modelYear;

    @Size(max = 255, message = "Đường dẫn video không được vượt quá 255 ký tự")
    private String videoUrl;
}
