package com.tayota.userservice.entity.appointment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


// Bảng này lưu thông tin về các ngày nghỉ của đại lý, để tránh việc khách hàng đặt lịch vào những ngày này.
// Mỗi bản ghi sẽ liên kết với một đại lý cụ thể (dealershipId) và có một ngày nghỉ cụ thể (holidayDate).
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "\"APPOINTMENT_HOLIDAY\"",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_appointment_holiday_dealership_date",
                        columnNames = {"dealership_id", "holiday_date"}
                )
        }
)
public class AppointmentHoliday {
    // Sử dụng UUID để dễ dàng mở rộng và tránh xung đột ID khi có nhiều bản ghi.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Mỗi ngày nghỉ sẽ liên kết với một đại lý cụ thể, giúp hệ thống biết được ngày nào đại lý đó không hoạt động.
    @Column(name = "dealership_id", nullable = false)
    private UUID dealershipId;

    // Ngày nghỉ cụ thể của đại lý, giúp hệ thống tránh việc khách hàng đặt lịch vào ngày này.
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    // Lý do đại lý nghỉ vào ngày này, giúp hiểu được nguyên nhân của ngày nghỉ.
    @Column(length = 255)
    private String reason;

    // Trạng thái của ngày nghỉ, giúp hệ thống biết được ngày nghỉ này có còn hiệu lực hay không.
    @Column(name = "is_active", nullable = false)
    private Boolean active;

    // Thông tin về thời điểm tạo và cập nhật bản ghi, giúp theo dõi lịch sử thay đổi của ngày nghỉ.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Thông tin về thời điểm cập nhật bản ghi, giúp theo dõi lịch sử thay đổi của ngày nghỉ.
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
