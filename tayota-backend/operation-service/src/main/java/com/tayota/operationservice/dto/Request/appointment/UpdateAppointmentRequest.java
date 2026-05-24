package com.tayota.operationservice.dto.Request.appointment;

import com.tayota.operationservice.enums.appointment.AppointmentStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

// Dùng để quản lý cập nhật thông tin lịch hẹn.
@Getter
public class UpdateAppointmentRequest {

    // Trạng thái của lịch hẹn
    private AppointmentStatus status;

    // Ngày hẹn
    private LocalDate appointmentDate;

    // Giờ bắt đầu của lịch hẹn
    private LocalTime startTime;

    // Đại lý
    private String dealershipId;

    // Ghi chú cho lịch hẹn, tối đa 500 ký tự
    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String notes;

    // Lý do hủy cuộc hẹn, tối đa 500 ký tự
    @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự")
    private String cancelReason;
}
