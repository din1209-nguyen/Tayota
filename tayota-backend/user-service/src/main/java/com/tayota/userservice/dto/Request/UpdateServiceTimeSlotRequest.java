package com.tayota.userservice.dto.Request;

import com.tayota.userservice.enums.AppointmentType;
import lombok.Getter;

import java.time.LocalTime;

// DTO này được sử dụng để nhận dữ liệu từ client khi cập nhật một khung giờ dịch vụ cho đại lý.
@Getter
public class UpdateServiceTimeSlotRequest {
    // Loại lịch hẹn, có thể để trống nếu không muốn cập nhật
    private AppointmentType appointmentType;

    // Giờ bắt đầu, có thể để trống nếu không muốn cập nhật
    private LocalTime startTime;

    // Giờ kết thúc, có thể để trống nếu không muốn cập nhật
    private LocalTime endTime;

    // Trạng thái của khung giờ, có thể để trống nếu không muốn cập nhật
    private Boolean active;
}
