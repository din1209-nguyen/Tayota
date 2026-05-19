package com.nguyendin.operationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "\"USER_REVIEW\"")
public class UserReview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false, unique = true)
    private ServiceTicket serviceTicket;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "mechanic_id", nullable = false)
    private UUID mechanicId;

    @Column(nullable = false)
    private Short rating; // Sao đánh giá từ 1 đến 5

    @Column(columnDefinition = "TEXT")
    private String comment; // Bình luận chi tiết về trải nghiệm dịch vụ.

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // Thời điểm tạo đánh giá, tự động được gán khi bản ghi được tạo ra.
}