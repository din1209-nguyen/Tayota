package com.tayota.userservice.entity;


import com.tayota.userservice.enums.AppointmentStatus;
import com.tayota.userservice.enums.AppointmentType;
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

    @Column(name = "car_version_id")
    private UUID carVersionId;

    @Column(name = "vin_id", length = 17)
    private String vinId;

    @Column(name = "dealership_id", nullable = false)
    private UUID dealershipId;

    @Column(name = "mechanic_id")
    private UUID mechanicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_information_id")
    private GuestInformation guestInformation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AppointmentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppointmentStatus status;

    @Column(name = "scheduled_date", nullable = false)
    private Instant scheduledDate;

    @Column(columnDefinition = "TEXT")
    private String notes; // Ghi chú thêm về cuộc hẹn.

    @Column(name = "confirmed_at")
    private Instant confirmedAt; // Thời điểm xác nhận cuộc hẹn

    @Column(name = "checked_in_at")
    private Instant checkedInAt; // Thời điểm khách hàng đến và check-in tại đại lý

    @Column(name = "completed_at")
    private Instant completedAt;  // Thời điểm hoàn thành dịch vụ

    @Column(name = "canceled_at")
    private Instant canceledAt; // Thời điểm hủy cuộc hẹn (nếu có)

    @Column(name = "expired_at")
    private Instant expiredAt; // Thời điểm hết hạn cuộc hẹn (nếu khách hàng không đến và không hủy trước)

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason; // Lý do hủy cuộc hẹn (nếu có)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // Thời điểm tạo cuộc hẹn, tự động được thiết lập bởi Hibernate và không thể cập nhật sau khi tạo

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // Thời điểm cập nhật cuộc hẹn, tự động được thiết lập bởi Hibernate và được cập nhật mỗi khi bản ghi được thay đổi
}