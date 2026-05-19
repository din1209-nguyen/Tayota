package com.nguyendin.operationservice.entity;

import com.nguyendin.operationservice.enums.ServiceTicketStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "\"SERVICE\"")
public class ServiceTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "vin_id", nullable = false, length = 17)
    private String vinId;

    @Column(name = "mechanic_id")
    private UUID mechanicId;

    @Column(name = "dealership_id", nullable = false)
    private UUID dealershipId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", unique = true)
    private Appointment appointment;

    @Column(name = "mileage_at_service")
    private Integer mileageAtService; // Số km của xe tại thời điểm mang đi bảo dưỡng/sửa chữa

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceTicketStatus status; // Trạng thái của phiếu dịch vụ: CONFIRMED, RECEIVED, IN_PROGRESS, COMPLETED, CANCELED, EXPIRED

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount; // Tổng chi phí của dịch vụ, có thể tính toán sau khi hoàn thành dịch vụ dựa trên các hạng mục công việc và phụ tùng đã sử dụng

    @Column(name = "vehicle_condition", columnDefinition = "TEXT")
    private String vehicleCondition; // Mô tả tình trạng xe khi mang đi bảo dưỡng/sửa chữa, có thể bao gồm các vấn đề đã gặp phải, tiếng động lạ, vết trầy xước, v.v.

    @Column(columnDefinition = "TEXT")
    private String notes; // Ghi chú thêm về dịch vụ, có thể do nhân viên kỹ thuật thêm vào để nhắc hẹn lần sau, hoặc để lại thông tin quan trọng về dịch vụ đã thực hiện.

    @Column(name = "receiving_at")
    private Instant receivingAt; // Thời điểm tiếp nhận xe tại đại lý.

    @Column(name = "processing_at")
    private Instant processingAt; // Thời điểm bắt đầu xử lý dịch vụ.

    @Column(name = "completed_at")
    private Instant completedAt; // Thời điểm hoàn thành dịch vụ.

    @Column(name = "canceled_at")
    private Instant canceledAt; // Thời điểm huỷ dịch vụ (nếu có)

    @Column(name = "expired_at")
    private Instant expiredAt; // Thời điểm hết hạn dịch vụ (nếu khách hàng không đến và không huỷ trước)

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason; // Lý do huỷ dịch vụ (nếu có), có thể do khách hàng cung cấp hoặc do đại lý ghi nhận khi huỷ dịch vụ.

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // Thời điểm tạo phiếu dịch vụ, tự động được thiết lập bởi Hibernate và không thể cập nhật sau khi tạo

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // Thời điểm cập nhật phiếu dịch vụ, tự động được thiết lập bởi Hibernate mỗi khi có sự thay đổi và lưu vào cơ sở dữ liệu
}