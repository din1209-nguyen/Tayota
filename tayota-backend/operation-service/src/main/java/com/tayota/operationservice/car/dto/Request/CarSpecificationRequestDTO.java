package com.tayota.operationservice.car.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CarSpecificationRequestDTO {
    @NotBlank(message = "Xuất xứ không được để trống")
    @Size(max = 100, message = "Xuất xứ không được vượt quá 100 ký tự")
    private String origin;

    @NotBlank(message = "Nhiên liệu không được để trống")
    @Size(max = 50, message = "Nhiên liệu không được vượt quá 50 ký tự")
    private String fuel;

    @NotNull(message = "Số ghế không được để trống")
    private Integer numberOfSeats;

    @NotNull(message = "Chiều dài không được để trống")
    private Integer length;

    @NotNull(message = "Chiều rộng không được để trống")
    private Integer width;

    @NotNull(message = "Chiều cao không được để trống")
    private Integer height;

    private Integer capacity;
    private String cylinderCapacity;
    private Integer cylinder;
    private String gearbox;
    private Integer maximumSpeed;
    private String acceleration;
    private String torque;
    private Integer grossWeightAllowance;
    private String trademarks;
}
