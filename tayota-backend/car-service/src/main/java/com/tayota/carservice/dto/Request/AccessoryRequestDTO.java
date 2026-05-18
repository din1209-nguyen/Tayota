package com.tayota.carservice.dto.Request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class AccessoryRequestDTO {
    @NotBlank(message = "Model phụ kiện không được để trống")
    @Size(max = 100, message = "Model phụ kiện không được vượt quá 100 ký tự")
    private String model;

    @NotBlank(message = "Thương hiệu không được để trống")
    @Size(max = 100, message = "Thương hiệu không được vượt quá 100 ký tự")
    private String brand;

    @NotNull(message = "Giá phụ kiện không được để trống")
    @DecimalMin(value = "0.00", message = "Giá phụ kiện không hợp lệ")
    private BigDecimal price;

    @NotBlank(message = "Mô tả phụ kiện không được để trống")
    @Size(max = 500, message = "Mô tả phụ kiện không được vượt quá 500 ký tự")
    private String description;

    @NotBlank(message = "Công dụng phụ kiện không được để trống")
    @Size(max = 500, message = "Công dụng phụ kiện không được vượt quá 500 ký tự")
    private String useContent;

    @NotBlank(message = "Lưu ý phụ kiện không được để trống")
    @Size(max = 500, message = "Lưu ý phụ kiện không được vượt quá 500 ký tự")
    private String reminderContent;

    @NotBlank(message = "Loại phụ kiện không được để trống")
    @Size(max = 100, message = "Loại phụ kiện không được vượt quá 100 ký tự")
    private String type;
}
