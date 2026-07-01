package com.tayota.operationservice.dto.request.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

// Dùng để lưu thông tin yêu cầu tạo cuộc hẹn dịch vụ từ phía khách hàng gửi lên
@Getter
public class CreateServiceAppointmentRequest {
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String guestFullName;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 250, message = "Email không được vượt quá 250 ký tự")
    private String guestEmail;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String guestPhone;

    @NotBlank(message = "Số VIN không được để trống")
    @Size(min = 17, max = 17, message = "Số VIN phải gồm 17 ký tự")
    private String vinId;

    @NotBlank(message = "Đại lý không được để trống")
    private String dealershipId;

    @NotNull(message = "Ngày hẹn không được để trống")
    private LocalDate appointmentDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotBlank(message = "Mô tả tình trạng xe không được để trống")
    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String notes;
}
