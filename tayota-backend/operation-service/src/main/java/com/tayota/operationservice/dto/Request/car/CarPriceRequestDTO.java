package com.tayota.operationservice.dto.request.car;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class CarPriceRequestDTO {
    @NotBlank(message = "Màu ngoại thất không được để trống")
    private String exteriorColorId;

    @NotBlank(message = "Màu nội thất không được để trống")
    private String interiorColorId;

    @NotNull(message = "Giá xe không được để trống")
    @DecimalMin(value = "0.00", message = "Giá xe không hợp lệ")
    private BigDecimal price;

    @Size(max = 255, message = "Đường dẫn ảnh ngoại thất không được vượt quá 255 ký tự")
    private String exImageUrl;

    @Size(max = 255, message = "Đường dẫn ảnh nội thất không được vượt quá 255 ký tự")
    private String inImageUrl;
}
