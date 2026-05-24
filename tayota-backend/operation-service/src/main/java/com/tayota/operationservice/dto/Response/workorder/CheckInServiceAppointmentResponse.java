package com.tayota.operationservice.dto.response.workorder;

import com.tayota.operationservice.dto.response.appointment.AppointmentManagementDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

// DTO để trả về thông tin chi tiết của lịch hẹn và phiếu dịch vụ sau khi khách hàng check-in
@Getter
@AllArgsConstructor
public class CheckInServiceAppointmentResponse {
    // Thông tin chi tiết của lịch hẹn đã được check-in
    private AppointmentManagementDetailResponse appointment;

    // Thông tin chi tiết của phiếu dịch vụ liên quan đến lịch hẹn đã check-in
    private ServiceTicketSummaryResponse serviceTicket;
}
