package com.tayota.operationservice.dto.Request.appointment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

// DTO này được sử dụng để nhận dữ liệu từ client khi tạo một ngày nghỉ mới cho đại lý.
@Getter
public class CreateAppointmentHolidayRequest {
    // Ngày nghỉ
    @NotNull(message = "Ngày nghỉ không được để trống")
    private LocalDate holidayDate;

    // Lý do nghỉ, tối đa 255 ký tự
    @Size(max = 255, message = "Lý do nghỉ không được vượt quá 255 ký tự")
    private String reason;

    // Trạng thái của ngày nghỉ, mặc định là true (còn hiệu lực)
    private Boolean active;
}
