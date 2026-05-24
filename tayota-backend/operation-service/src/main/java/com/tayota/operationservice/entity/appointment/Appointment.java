package com.tayota.operationservice.entity.appointment;


import com.tayota.operationservice.enums.appointment.AppointmentStatus;
import com.tayota.operationservice.enums.appointment.AppointmentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "\"APPOINTMENT\"")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Có tài khoản thì lưu userId từ JWT. Khách vãng lai thì để null và dùng guestInformation.
    @Column(name = "user_id")
    private UUID userId;

    // Thông tin về phiên bản xe, dành cho đơn đăng ký lái thử.
    @Column(name = "car_version_id")
    private UUID carVersionId;

    // Thông tin về xe của khách hàng, dành cho đơn đăng ký dịch vụ.
    @Column(name = "vin_id", length = 17)
    private String vinId;

    // Thông tin về đại lý, dành cho cả đơn đăng ký lái thử và dịch vụ.
    @Column(name = "dealership_id", nullable = false)
    private UUID dealershipId;

    // Thông tin về kỹ thuật viên.
    @Column(name = "mechanic_id")
    private UUID mechanicId;

    // Thông tin về khách hàng, dành cho khách vãng lai.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_information_id")
    private GuestInformation guestInformation;

    // Loại cuộc hẹn: lái thử, dịch vụ, bảo dưỡng, v.v. Có thể dùng để phân biệt các loại cuộc hẹn khác nhau và áp dụng logic xử lý phù hợp cho từng loại.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AppointmentType type;

    // Trạng thái cuộc hẹn: đã xác nhận, đã hủy, đã hoàn thành, v.v. Có thể dùng để theo dõi tiến trình của cuộc hẹn và áp dụng logic xử lý phù hợp dựa trên trạng thái hiện tại của cuộc hẹn.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppointmentStatus status;

    // Thời điểm dự kiến bắt đầu cuộc hẹn
    @Column(name = "scheduled_start_at", nullable = false)
    private Instant scheduledStartAt;

    // Thời điểm dự kiến kết thúc cuộc hẹn
    @Column(name = "scheduled_end_at", nullable = false)
    private Instant scheduledEndAt;

    // Ghi chú về tình trạng xe hiện tại, dành cho đơn đăng ký dịch vụ. Có thể chứa thông tin về tình trạng xe.
    @Column(columnDefinition = "TEXT")
    private String notes; // Ghi chú thêm về cuộc hẹn.

    // Thời điểm xác nhận cuộc hẹn
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "completed_at")
    private Instant completedAt;  // Thời điểm hoàn thành dịch vụ

    @Column(name = "canceled_at")
    private Instant canceledAt; // Thời điểm hủy cuộc hẹn (nếu có)

    @Column(name = "expired_at")
    private Instant expiredAt; // Thời điểm hết hạn cuộc hẹn (nếu khách hàng không đến và không hủy trước)

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason; // Lý do hủy cuộc hẹn (nếu có)

    // Thời điểm tạo cuộc hẹn, tự động được thiết lập bởi Hibernate và không thể cập nhật sau khi tạo
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Thời điểm cập nhật cuộc hẹn, tự động được thiết lập bởi Hibernate và được cập nhật mỗi khi bản ghi được thay đổi
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}