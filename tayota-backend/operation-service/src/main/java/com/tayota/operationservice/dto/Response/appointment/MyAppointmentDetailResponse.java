package com.tayota.operationservice.dto.response.appointment;

import com.tayota.operationservice.enums.appointment.AppointmentStatus;
import com.tayota.operationservice.enums.appointment.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// Dùng để trả về chi tiết lịch hẹn cho user đang đăng nhập.
@Getter
@AllArgsConstructor
public class MyAppointmentDetailResponse {
    // ID của lịch hẹn
    private UUID id;

    // Loại của lịch hẹn (Service, Test Drive)
    private AppointmentType type;

    // Trạng thái của lịch hẹn (Pending, Confirmed, Completed, Canceled, Expired)
    private AppointmentStatus status;

    // Ngày hẹn
    private LocalDate appointmentDate;

    // Giờ bắt đầu của lịch hẹn
    private LocalTime startTime;

    // Giờ kết thúc của lịch hẹn
    private LocalTime endTime;

    // ID của đại lý liên quan đến lịch hẹn
    private UUID dealershipId;

    // ID của phiên bản xe liên quan đến lịch hẹn (dành cho lịch hẹn Test Drive)
    private UUID carVersionId;

    // Số khung xe (VIN) liên quan đến lịch hẹn (dành cho lịch hẹn Service)
    private String vinId;

    // Tên đầy đủ của khách hàng
    private String notes;

    private String cancelReason;

    // Thông tin thời gian tạo và cập nhật của cuộc hẹn
    private Instant createdAt;

    // Thông tin thời gian cập nhật của cuộc hẹn
    private Instant updatedAt;
}
