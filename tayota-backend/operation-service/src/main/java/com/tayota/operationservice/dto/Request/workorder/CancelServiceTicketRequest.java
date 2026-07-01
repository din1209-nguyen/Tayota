package com.tayota.operationservice.dto.request.workorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CancelServiceTicketRequest {
    @NotBlank(message = "Vui lòng nhập lý do hủy phiếu dịch vụ")
    @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự")
    private String reason;
}
