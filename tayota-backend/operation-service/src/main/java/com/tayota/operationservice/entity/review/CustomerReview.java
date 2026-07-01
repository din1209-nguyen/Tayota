package com.tayota.operationservice.entity.review;

import com.tayota.operationservice.entity.appointment.Appointment;
import com.tayota.operationservice.entity.workorder.ServiceTicket;
import com.tayota.operationservice.enums.review.ReviewStatus;
import com.tayota.operationservice.enums.review.ReviewType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

// Bảng "CUSTOMER_REVIEW" lưu trữ thông tin đánh giá của khách hàng về dịch vụ và thợ sửa xe sau khi hoàn thành.
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "\"CUSTOMER_REVIEW\"",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customer_review_appointment", columnNames = "appointment_id"),
                @UniqueConstraint(name = "uk_customer_review_service", columnNames = "service_id"),
                @UniqueConstraint(name = "uk_customer_review_token", columnNames = "review_token")
        }
)
public class CustomerReview {
    // ID của đánh giá, được tự động sinh ra dưới dạng UUID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Loại đánh giá, có thể là đánh giá về dịch vụ hoặc đánh giá về thợ sửa xe
    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false, length = 30)
    private ReviewType reviewType;

    // Trạng thái của đánh giá
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewStatus status;

    // Mỗi đánh giá sẽ có một mã token duy nhất được tạo ra khi khách hàng tạo đánh giá, giúp xác thực và liên kết đánh giá với lịch hẹn hoặc phiếu dịch vụ cụ thể.
    @Column(name = "review_token", nullable = false, length = 80)
    private String reviewToken;

    // Thời điểm hết hạn của token đánh giá, giúp hệ thống tự động vô hiệu hóa các đánh giá cũ sau một khoảng thời gian nhất định.
    @Column(name = "token_expires_at", nullable = false)
    private Instant tokenExpiresAt;

    // Thời điểm khách hàng gửi đánh giá, được tự động gán khi khách hàng tạo đánh giá và không thể cập nhật sau đó.
    @Column(name = "submitted_at")
    private Instant submittedAt;

    // Mỗi đánh giá chỉ liên kết với một lịch hẹn hoặc một phiếu dịch vụ, đảm bảo rằng khách hàng chỉ có thể đánh giá một lần cho mỗi lịch hẹn hoặc phiếu dịch vụ.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    // Mỗi đánh giá chỉ liên kết với một lịch hẹn hoặc một phiếu dịch vụ, đảm bảo rằng khách hàng chỉ có thể đánh giá một lần cho mỗi lịch hẹn hoặc phiếu dịch vụ.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceTicket serviceTicket;

    // ID của khách hàng (người dùng) đã tạo đánh giá, có thể null nếu khách hàng không đăng nhập khi tạo đánh giá.
    @Column(name = "user_id")
    private UUID userId;

    // Thông tin liên hệ của khách hàng tại thời điểm tạo đánh giá khi khách hàng không đăng nhập.
    @Column(name = "guest_full_name", length = 100)
    private String guestFullName;

    // Email của khách hàng tại thời điểm tạo đánh giá khi khách hàng không đăng nhập.
    @Column(name = "guest_email", length = 120)
    private String guestEmail;

    // Số điện thoại của khách hàng tại thời điểm tạo đánh giá khi khách hàng không đăng nhập.
    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    // ID của đại lý mà khách hàng đã sử dụng dịch vụ, giúp liên kết đánh giá với đại lý cụ thể.
    @Column(name = "dealership_id", nullable = false)
    private UUID dealershipId;

    // ID của xe mà khách hàng đã sử dụng dịch vụ, giúp liên kết đánh giá với xe cụ thể.
    @Column(name = "service_rating")
    private Short serviceRating;

    // Ghi chú bổ sung cho đánh giá về dịch vụ, có thể null nếu khách hàng không cung cấp ghi chú.
    @Column(name = "service_comment", columnDefinition = "TEXT")
    private String serviceComment;

    // ID của thợ sửa xe mà khách hàng đã đánh giá, giúp liên kết đánh giá với thợ sửa xe cụ thể.
    @Column(name = "mechanic_id")
    private UUID mechanicId;

    // Số sao đánh giá về thợ sửa xe, ví dụ: 4.5, giúp khách hàng thể hiện mức độ hài lòng với thợ sửa xe.
    @Column(name = "mechanic_rating")
    private Short mechanicRating;

    // Ghi chú bổ sung cho đánh giá về thợ sửa xe, có thể null nếu khách hàng không cung cấp ghi chú.
    @Column(name = "mechanic_comment", columnDefinition = "TEXT")
    private String mechanicComment;

    // Thời điểm tạo đánh giá, được tự động gán khi bản ghi được tạo ra và không thể cập nhật sau đó.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
