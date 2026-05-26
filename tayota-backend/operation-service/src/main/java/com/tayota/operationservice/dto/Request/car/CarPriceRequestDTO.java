package com.tayota.operationservice.dto.request.car;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class CarPriceRequestDTO {
    private String exteriorColorId;

    @Size(max = 50, message = "Tên màu ngoại thất không được vượt quá 50 ký tự")
    private String exteriorColorName;

    private String interiorColorId;

    @Size(max = 50, message = "Tên màu nội thất không được vượt quá 50 ký tự")
    private String interiorColorName;

    @NotNull(message = "Giá xe không được để trống")
    @DecimalMin(value = "0.00", message = "Giá xe không hợp lệ")
    private BigDecimal price;

    @Size(max = 1024, message = "Đường dẫn ảnh ngoại thất không được vượt quá 1024 ký tự")
    private String exImageUrl;

    @Size(max = 1024, message = "Đường dẫn ảnh nội thất không được vượt quá 1024 ký tự")
    private String inImageUrl;

    @AssertTrue(message = "Màu ngoại thất không được để trống")
    public boolean isExteriorColorProvided() {
        return hasText(exteriorColorId) || hasText(exteriorColorName);
    }

    @AssertTrue(message = "Màu nội thất không được để trống")
    public boolean isInteriorColorProvided() {
        return hasText(interiorColorId) || hasText(interiorColorName);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
