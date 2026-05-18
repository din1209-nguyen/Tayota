package com.tayota.carservice.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CarSeriesRequestDTO {
    @NotBlank(message = "Kiểu dáng không được để trống")
    private String carStyleId;

    @NotBlank(message = "Tên dòng xe không được để trống")
    @Size(max = 100, message = "Tên dòng xe không được vượt quá 100 ký tự")
    private String name;

    @NotBlank(message = "Mô tả dòng xe không được để trống")
    @Size(max = 250, message = "Mô tả dòng xe không được vượt quá 250 ký tự")
    private String description;
}
