package com.tayota.userservice.repository.review;

import com.tayota.userservice.entity.review.CustomerReview;
import com.tayota.userservice.enums.review.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerReviewRepository extends JpaRepository<CustomerReview, UUID> {
    // Kiểm tra xem đã tồn tại đánh giá nào cho một appointment cụ thể chưa
    boolean existsByAppointment_Id(UUID appointmentId);

    // Kiểm tra xem đã tồn tại đánh giá nào cho một service ticket cụ thể chưa
    boolean existsByServiceTicket_Id(UUID serviceTicketId);

    // Lấy tất cả đánh giá của một người dùng, sắp xếp theo thời gian tạo mới nhất
    List<CustomerReview> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<CustomerReview> findByReviewToken(String reviewToken);

    // Lấy review token của đánh giá liên quan đến một appointment cụ thể, nếu có
    @Query("""
            select review.reviewToken
            from CustomerReview review
            where review.appointment.id = :appointmentId
            """)
    Optional<String> findByReviewTokenForAppointment(@Param("appointmentId") UUID appointmentId);

    // Lấy review token của đánh giá liên quan đến một service ticket cụ thể, nếu có
    @Query("""
            select review.reviewToken
            from CustomerReview review
            where review.serviceTicket.id = :serviceTicketId
            """)
    Optional<String> findByReviewTokenForServiceTicket(@Param("serviceTicketId") UUID serviceTicketId);

    // Lấy tất cả đánh giá liên quan đến một thợ sửa xe cụ thể, sắp xếp theo thời gian tạo mới nhất
    @Query("""
            select avg(review.mechanicRating)
            from CustomerReview review
            where review.mechanicId = :mechanicId
              and review.mechanicRating is not null
            """)
    Double findAverageMechanicRating(@Param("mechanicId") UUID mechanicId);

    // Tự động chuyển các đánh giá có trạng thái PENDING nhưng đã quá thời gian tokenExpiresAt sang EXPIRED.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CustomerReview review
            set review.status = :expiredStatus
            where review.status = :pendingStatus
              and review.tokenExpiresAt < :now
            """)
    int expirePendingReviews(
            @Param("pendingStatus") ReviewStatus pendingStatus,
            @Param("expiredStatus") ReviewStatus expiredStatus,
            @Param("now") Instant now
    );
}
