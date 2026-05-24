package com.tayota.operationservice.dto.response.workorder;

import com.tayota.operationservice.enums.workorder.ServiceTicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// DTO để trả về thông tin chi tiết của phiếu dịch vụ, bao gồm tất cả các trường dữ liệu liên quan đến phiếu dịch vụ
@Getter
@AllArgsConstructor
public class ServiceTicketInfoResponse {
    // ID của phiếu dịch vụ
    private UUID id;

    // ID của lịch hẹn liên quan đến phiếu dịch vụ
    private UUID appointmentId;

    // ID thông tin khách vãng lai, dùng cho guest hoặc service walk-in nếu không có tài khoản
    private UUID guestInformationId;

    // ID của xe (VIN) liên quan đến phiếu dịch vụ
    private String vinId;

    // ID của thợ sửa xe được chỉ định cho phiếu dịch vụ, có thể null nếu chưa được chỉ định
    private UUID mechanicId;

    // Số km của xe tại thời điểm nhận dịch vụ, có thể null nếu chưa được cập nhật
    private Integer mileageAtService;

    // Trạng thái hiện tại của phiếu dịch vụ
    private ServiceTicketStatus status;

    // Tình trạng của xe tại thời điểm nhận dịch vụ
    private String vehicleCondition;

    // Ghi chú bổ sung cho phiếu dịch vụ, có thể null nếu không có ghi chú
    private String notes;

    // Thời điểm phiếu dịch vụ được nhận, có thể null nếu chưa được cập nhật
    private Instant receivingAt;

    // Thời điểm phiếu dịch vụ bắt đầu được xử lý, có thể null nếu chưa được cập nhật
    private Instant processingAt;

    // Thời điểm phiếu dịch vụ được hoàn thành, có thể null nếu chưa được cập nhật
    private Instant completedAt;

    // Tổng số tiền của phiếu dịch vụ, có thể null nếu chưa được cập nhật hoặc nếu phiếu dịch vụ chưa có hạng mục nào
    private BigDecimal totalAmount;

}
