package com.tayota.operationservice.dto.request.workorder;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CreateWalkInServiceTicketRequest {
    private UUID userId;

    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String guestFullName;

    @Email(message = "Email không hợp lệ")
    @Size(max = 250, message = "Email không được vượt quá 250 ký tự")
    private String guestEmail;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String guestPhone;

    @Size(min = 17, max = 17, message = "Số VIN phải gồm 17 ký tự")
    private String vinId;

    private UUID mechanicId;

    @Min(value = 0, message = "Số km không được âm")
    private Integer mileageAtService;

    @Size(max = 1000, message = "Tình trạng xe không được vượt quá 1000 ký tự")
    private String vehicleCondition;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String notes;
}
