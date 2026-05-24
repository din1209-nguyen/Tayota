package com.tayota.operationservice.dto.Request.appointment;

import com.tayota.operationservice.enums.appointment.AppointmentType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalTime;

// DTO này được sử dụng để nhận dữ liệu từ client khi tạo một khung giờ dịch vụ mới cho đại lý.
@Getter
public class CreateServiceTimeSlotRequest {
    // Loại lịch hẹn, không được để trống
    @NotNull(message = "Loại lịch hẹn không được để trống")
    private AppointmentType appointmentType;

    // Giờ bắt đầu, không được để trống
    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    // Giờ kết thúc, không được để trống
    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;

    // Trạng thái của khung giờ, mặc định là true (còn hiệu lực)
    private Boolean active;
}
