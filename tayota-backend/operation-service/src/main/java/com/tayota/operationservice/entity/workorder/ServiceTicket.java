package com.tayota.operationservice.entity.workorder;


import com.tayota.operationservice.entity.appointment.Appointment;
import com.tayota.operationservice.entity.appointment.GuestInformation;
import com.tayota.operationservice.enums.workorder.ServiceTicketStatus;
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
    // ID của phiếu dịch vụ, sử dụng UUID để đảm bảo tính duy nhất trên toàn hệ thống và dễ dàng tích hợp với các hệ thống khác nếu cần thiết
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ID của người dùng (khách hàng) liên quan đến phiếu dịch vụ, null nếu là khách vãng lai hoặc service walk-in
    @Column(name = "user_id")
    private UUID userId;

    // ID thông tin khách vãng lai, dùng cho guest hoặc service walk-in nếu không có tài khoản, null nếu là khách hàng đã đăng ký tài khoản
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_information_id")
    private GuestInformation guestInformation;

    // ID của xe (VIN) liên quan đến phiếu dịch vụ, null nếu chưa được cập nhật hoặc nếu khách hàng chưa cung cấp thông tin xe
    @Column(name = "vin_id", nullable = false, length = 17)
    private String vinId;

    // ID của thợ sửa xe được chỉ định cho phiếu dịch vụ
    @Column(name = "mechanic_id")
    private UUID mechanicId;


    // ID của đại lý nơi phiếu dịch vụ được tạo ra, dùng để phân biệt các phiếu dịch vụ thuộc về các đại lý khác nhau trong hệ thống
    @Column(name = "dealership_id", nullable = false)
    private UUID dealershipId;

    // Mối quan hệ một-một giữa phiếu dịch vụ và lịch hẹn, mỗi phiếu dịch vụ chỉ liên quan đến một lịch hẹn duy nhất
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", unique = true)
    private Appointment appointment;

    // Số km của xe tại thời điểm nhận dịch vụ
    @Column(name = "mileage_at_service")
    private Integer mileageAtService;

    // Trạng thái của phiếu dịch vụ: CONFIRMED, RECEIVED, IN_PROGRESS, COMPLETED, CANCELED, EXPIRED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceTicketStatus status;

    // Tổng chi phí của dịch vụ, có thể tính toán sau khi hoàn thành dịch vụ dựa trên các hạng mục công việc và phụ tùng đã sử dụng
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    // Mô tả tình trạng xe khi mang đi bảo dưỡng/sửa chữa, có thể bao gồm các vấn đề đã gặp phải, tiếng động lạ, vết trầy xước, v.v.
    @Column(name = "vehicle_condition")
    private String vehicleCondition;

    // Ghi chú thêm về dịch vụ, có thể do nhân viên kỹ thuật thêm vào để nhắc hẹn lần sau, hoặc để lại thông tin quan trọng về dịch vụ đã thực hiện.
    private String notes;

    // Thời điểm tiếp nhận xe tại đại lý.
    @Column(name = "receiving_at")
    private Instant receivingAt;

    // Thời điểm bắt đầu xử lý dịch vụ.
    @Column(name = "processing_at")
    private Instant processingAt;

    // Thời điểm hoàn thành dịch vụ.
    @Column(name = "completed_at")
    private Instant completedAt;

    // Thời điểm huỷ dịch vụ (nếu có)
    @Column(name = "canceled_at")
    private Instant canceledAt;

    // Thời điểm hết hạn dịch vụ (nếu khách hàng không đến và không huỷ trước)
    @Column(name = "expired_at")
    private Instant expiredAt;

    // Lý do huỷ dịch vụ (nếu có), có thể do khách hàng cung cấp hoặc do đại lý ghi nhận khi huỷ dịch vụ.
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    // Thời điểm tạo phiếu dịch vụ, tự động được thiết lập bởi Hibernate và không thể cập nhật sau khi tạo
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Thời điểm cập nhật phiếu dịch vụ, tự động được thiết lập bởi Hibernate mỗi khi có sự thay đổi và lưu vào cơ sở dữ liệu
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
