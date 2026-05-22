package com.tayota.userservice.entity;

import com.tayota.userservice.enums.AppointmentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

// Bảng này lưu thông tin về các khung giờ làm việc của đại lý, để hệ thống biết được khi nào đại lý có thể nhận cuộc hẹn.
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "\"SERVICE_TIME_SLOT\"",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_service_time_slot_dealership_type_start",
                        columnNames = {"dealership_id", "appointment_type", "start_time"}
                )
        }
)
public class ServiceTimeSlot {
    // Sử dụng UUID để dễ dàng mở rộng và tránh xung đột ID khi có nhiều bản ghi.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Mỗi khung giờ làm việc sẽ liên kết với một đại lý cụ thể, giúp hệ thống biết được khung giờ này thuộc về đại lý nào.
    @Column(name = "dealership_id", nullable = false)
    private UUID dealershipId;

    // Loại cuộc hẹn mà khung giờ này áp dụng, giúp hệ thống biết được khung giờ này dành cho loại cuộc hẹn nào (lái thử, dịch vụ, bảo dưỡng, v.v.).
    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_type", nullable = false, length = 40)
    private AppointmentType appointmentType;

    // Thời gian bắt đầu của khung giờ làm việc.
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    // Thời gian kết thúc của khung giờ làm việc.
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // Trạng thái của khung giờ làm việc, giúp hệ thống biết được khung giờ này có còn hiệu lực hay không.
    @Column(name = "is_active", nullable = false)
    private Boolean active;

    // Thông tin về thời điểm tạo bản ghi, giúp theo dõi lịch sử thay đổi của khung giờ làm việc.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Thông tin về thời điểm cập nhật bản ghi, giúp theo dõi lịch sử thay đổi của khung giờ làm việc.
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
