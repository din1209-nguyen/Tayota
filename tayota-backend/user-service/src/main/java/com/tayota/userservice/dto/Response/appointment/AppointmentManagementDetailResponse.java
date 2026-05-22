package com.tayota.userservice.dto.Response.appointment;

import com.tayota.userservice.enums.appointment.AppointmentStatus;
import com.tayota.userservice.enums.appointment.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// Dùng để trả về chi tiết lịch hẹn cho quản lý/admin.
@Getter
@AllArgsConstructor
public class AppointmentManagementDetailResponse {
    // ID của lịch hẹn
    private UUID id;

    // ID của người dùng đặt lịch hẹn
    private UUID userId;

    // Là user đã đăng nhập hay là khách
    private String customerType;

    // Tên đầy đủ của khách hàng
    private String customerFullName;

    // Địa chỉ email của khách hàng
    private String customerEmail;

    // Số điện thoại của khách hàng
    private String customerPhone;

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

    // Ghi chú cho lịch hẹn
    private String notes;

    // Thời gian xác nhận của lịch hẹn
    private Instant confirmedAt;

    // Thời gian hoàn thành của lịch hẹn
    private Instant completedAt;

    // Thời gian hủy của lịch hẹn
    private Instant canceledAt;

    // Thời gian hết hạn của lịch hẹn
    private Instant expiredAt;

    // Lý do hủy của lịch hẹn (nếu có)
    private String cancelReason;

    // Thời gian tạo của lịch hẹn
    private Instant createdAt;

    // Thời gian cập nhật của lịch hẹn
    private Instant updatedAt;

}
