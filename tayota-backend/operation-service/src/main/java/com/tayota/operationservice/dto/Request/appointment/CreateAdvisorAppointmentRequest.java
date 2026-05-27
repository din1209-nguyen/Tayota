package com.tayota.operationservice.dto.request.appointment;

import com.tayota.operationservice.enums.appointment.AppointmentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// Dùng để cố vấn dịch vụ tạo lịch hẹn thay cho khách gọi qua tổng đài.
@Getter
public class CreateAdvisorAppointmentRequest {
    private UUID userId;

    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String guestFullName;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 250, message = "Email không được vượt quá 250 ký tự")
    private String guestEmail;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String guestPhone;

    @NotNull(message = "Loại lịch không được để trống")
    private AppointmentType appointmentType;

    private String vinId;

    private String carVersionId;

    @NotNull(message = "Ngày hẹn không được để trống")
    private LocalDate appointmentDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String notes;
}
