package com.nguyendin.operationservice.dto.request;

import com.nguyendin.operationservice.enums.AppointmentType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.Instant;

@Getter
public class CreateServiceAppointmentRequest {
    private String guestFullName;
    private String guestEmail;
    private String guestPhone;

    @NotBlank(message = "Số VIN không được để trống")
    @Size(max = 17, message = "Số VIN không được vượt quá 17 ký tự")
    private String vinId;

    private String carVersionId;

    @NotBlank(message = "Đại lý không được để trống")
    private String dealershipId;

    private String mechanicId;

    @NotNull(message = "Loại lịch hẹn không được để trống")
    private AppointmentType type;

    @NotNull(message = "Ngày hẹn không được để trống")
    @Future(message = "Ngày hẹn phải ở tương lai")
    private Instant scheduledDate;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String notes;
}