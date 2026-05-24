package com.tayota.operationservice.car.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CarStyleRequestDTO {
    @NotBlank(message = "Tên kiểu xe không được để trống")
    @Size(max = 100, message = "Tên kiểu xe không được vượt quá 100 ký tự")
    private String name;

    @NotBlank(message = "Mô tả kiểu xe không được để trống")
    @Size(max = 250, message = "Mô tả kiểu xe không được vượt quá 250 ký tự")
    private String description;
}
