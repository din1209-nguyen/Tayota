package com.tayota.userservice.dto.Request;

import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

// DTO này được sử dụng để nhận dữ liệu từ client khi cập nhật một ngày nghỉ cho đại lý.
@Getter
public class UpdateAppointmentHolidayRequest {
    // Ngày nghỉ, có thể để trống nếu không muốn cập nhật
    private LocalDate holidayDate;

    // Lý do nghỉ, tối đa 255 ký tự, có thể để trống nếu không muốn cập nhật
    @Size(max = 255, message = "Lý do nghỉ không được vượt quá 255 ký tự")
    private String reason;

    // Trạng thái của ngày nghỉ, có thể để trống nếu không muốn cập nhật
    private Boolean active;
}
