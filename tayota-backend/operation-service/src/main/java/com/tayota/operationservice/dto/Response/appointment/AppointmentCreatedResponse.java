package com.tayota.operationservice.dto.response.appointment;

import com.tayota.operationservice.enums.appointment.AppointmentStatus;
import com.tayota.operationservice.enums.appointment.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

// Dùng để lưu thông tin phản hồi sau khi tạo cuộc hẹn thành công
@Getter
@AllArgsConstructor
public class AppointmentCreatedResponse {
    private UUID id;
    private AppointmentType type;
    private AppointmentStatus status;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime createdAt;
}
