package com.tayota.operationservice.dto.Request.workorder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.UUID;

// Dùng để nhận dữ liệu khách hàng check-in khi đến xưởng (đặt lịch dịch vụ)
// bao gồm thông tin về thợ sửa, số km hiện tại, tình trạng xe và ghi chú nếu có
@Getter
public class CheckInServiceAppointmentRequest {
    private UUID mechanicId;

    @Min(value = 0, message = "Số km không được âm")
    private Integer mileageAtService;

    @Size(max = 1000, message = "Tình trạng xe không được vượt quá 1000 ký tự")
    private String vehicleCondition;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String notes;
}
