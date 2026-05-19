package com.nguyendin.operationservice.entity;

import com.nguyendin.operationservice.enums.ReminderStatus;
import com.nguyendin.operationservice.enums.ReminderType;
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
@Table(name = "\"APPOINTMENT_REMINDER\"")
public class AppointmentReminder {
    // dùng để lưu trữ thông tin các lời nhắc hẹn của cuộc hẹn, có thể là lời nhắc qua email, hoặc thông báo để báo cho khách hàng biết về cuộc hẹn sắp tới, cũng như trạng thái của lời nhắc (đã gửi, chưa gửi, đã hủy, v.v.)

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReminderType type; // Loại lời nhắc: BEFORE_24H, BEFORE_2H, THANK_YOU, REVIEW_REQUEST

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReminderStatus status;

    @Column(name = "remind_at", nullable = false)
    private Instant remindAt; // Thời điểm lời nhắc được lên lịch để gửi đến khách hàng.

    @Column(name = "sent_at")
    private Instant sentAt; // Thời điểm lời nhắc thực sự được gửi đi. Có thể null nếu lời nhắc chưa được gửi.

    @Column(name = "retry_count")
    private Integer retryCount; // Số lần đã thử gửi lời nhắc. Có thể dùng để theo dõi và quyết định khi nào nên ngừng thử gửi lại nếu gặp lỗi.

    @Column(nullable = false, length = 250)
    private String email; // Địa chỉ email của khách hàng nhận lời nhắc. Có thể dùng để gửi lời nhắc qua email hoặc để xác định người nhận trong hệ thống thông báo.

    @Column(nullable = false, length = 255)
    private String subject; // Tiêu đề của lời nhắc, có thể dùng để phân biệt các loại lời nhắc khác nhau hoặc để hiển thị trong thông báo.

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body; // Nội dung chi tiết của lời nhắc, có thể chứa thông tin về cuộc hẹn, hướng dẫn, hoặc bất kỳ thông tin nào cần thiết để khách hàng biết khi nhận được lời nhắc.

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // Thời điểm tạo lời nhắc, tự động được gán khi bản ghi được tạo ra.

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // Thời điểm cập nhật lời nhắc, tự động được gán mỗi khi bản ghi được cập nhật.
}