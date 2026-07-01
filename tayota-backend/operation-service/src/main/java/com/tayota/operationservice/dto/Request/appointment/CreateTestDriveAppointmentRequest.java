package com.tayota.operationservice.dto.request.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateTestDriveAppointmentRequest {
    @Size(max = 100)
    private String guestFullName;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 250)
    private String guestEmail;

    @Size(max = 20)
    private String guestPhone;

    @NotBlank(message = "Phiên bản xe không được để trống")
    private String carVersionId;

    @NotBlank(message = "Đại lý không được để trống")
    private String dealershipId;

    @NotNull(message = "Ngày hẹn không được để trống")
    private LocalDate appointmentDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String notes;
}
