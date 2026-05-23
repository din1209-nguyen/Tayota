package com.tayota.userservice.dto.Response.workorder;

import com.tayota.userservice.enums.workorder.ServiceTicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ServiceTicketSummaryResponse {
    // ID của phiếu dịch vụ
    private UUID id;

    // ID của lịch hẹn liên quan đến phiếu dịch vụ
    private UUID appointmentId;

    // Biển số xe của khách hàng, được lấy từ bảng Vehicle dựa trên VIN ID
    private String vinId;

    // ID của thợ sửa xe được chỉ định cho phiếu dịch vụ, có thể null nếu chưa được chỉ định
    private UUID mechanicId;

    // Số km của xe tại thời điểm khách hàng check-in, được lấy từ yêu cầu check-in
    private Integer mileageAtService;

    // Trạng thái hiện tại của phiếu dịch vụ, ví dụ: CONFIRMED, RECEIVING, PROCESSING, COMPLETED, CANCELED, EXPIRED
    private ServiceTicketStatus status;

    // Tình trạng xe tại thời điểm khách hàng check-in, được lấy từ yêu cầu check-in
    private String vehicleCondition;

    // Ghi chú của khách hàng tại thời điểm check-in, được lấy từ yêu cầu check-in
    private String notes;

    // Thời gian mà khách hàng check-in và phiếu dịch vụ được tạo ra, được lấy từ thời điểm tạo phiếu dịch vụ
    private Instant receivingAt;
}
