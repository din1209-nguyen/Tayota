package com.tayota.operationservice.entity;

import com.tayota.operationservice.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

// Lưu thông tin về các thông báo gửi đến người dùng.
// Có thể là thông báo về cuộc hẹn, khuyến mãi, hoặc các sự kiện khác liên quan đến dịch vụ
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "\"NOTIFICATION\"")
public class Notification {
    // Sử dụng UUID để dễ dàng mở rộng và tránh xung đột ID khi có nhiều bản ghi.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Mỗi thông báo sẽ liên kết với một người dùng cụ thể, giúp hệ thống biết được ai là người nhận của thông báo này
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Thông tin về người gửi thông báo, có thể là hệ thống hoặc một nhân viên cụ thể. Có thể để null nếu thông báo được gửi tự động từ hệ thống.
    @Column(name = "sender_id")
    private UUID senderId;

    // Loại thông báo, giúp hệ thống phân loại và xử lý các thông báo khác nhau một cách phù hợp. Ví dụ: APPOINTMENT_REMINDER, PROMOTION, SYSTEM_ALERT, v.v.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    // Tiêu đề của thông báo, giúp người dùng nhanh chóng hiểu được nội dung chính của thông báo khi nhận được.
    @Column(nullable = false, length = 250)
    private String title;

    // Nội dung chi tiết của thông báo, có thể chứa thông tin về cuộc hẹn, khuyến mãi, hoặc bất kỳ thông tin nào cần thiết để người dùng biết khi nhận được thông báo.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Trạng thái của thông báo, giúp hệ thống biết được thông báo này đã được người dùng đọc hay chưa. Có thể dùng để hiển thị số lượng thông báo chưa đọc cho người dùng.
    @Column(name = "is_read", nullable = false)
    private Boolean read;

    // Thời điểm người dùng đọc thông báo, có thể dùng để theo dõi và phân tích hành vi của người dùng khi tương tác với các thông báo.
    @Column(name = "read_at")
    private Instant readAt;

    // Thông tin về thời điểm tạo thông báo, tự động được gán khi bản ghi được tạo ra. Có thể dùng để sắp xếp và hiển thị thông báo theo thứ tự thời gian.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}