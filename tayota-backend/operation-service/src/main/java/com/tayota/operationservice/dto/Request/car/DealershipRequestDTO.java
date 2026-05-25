package com.tayota.operationservice.dto.request.car;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class DealershipRequestDTO {
    @NotBlank(message = "Tên đại lý không được để trống")
    @Size(max = 150, message = "Tên đại lý không được vượt quá 150 ký tự")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @NotNull(message = "Vĩ độ không được để trống")
    private BigDecimal latitude;

    @NotNull(message = "Kinh độ không được để trống")
    private BigDecimal longitude;

    @Size(max = 255, message = "Place ID không được vượt quá 255 ký tự")
    private String placeId;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    @Size(max = 100, message = "Giờ hoạt động không được vượt quá 100 ký tự")
    private String operatingHours;

    private Boolean active = true;
}
