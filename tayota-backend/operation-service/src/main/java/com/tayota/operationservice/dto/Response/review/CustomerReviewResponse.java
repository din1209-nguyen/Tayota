package com.tayota.operationservice.dto.Response.review;

import com.tayota.operationservice.enums.review.ReviewType;
import com.tayota.operationservice.enums.review.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

// DTO để trả về thông tin chi tiết của đánh giá của khách hàng, bao gồm tất cả các trường dữ liệu liên quan đến đánh giá
@Getter
@AllArgsConstructor
public class CustomerReviewResponse {
    // ID của đánh giá
    private UUID id;

    // Loại đánh giá.
    private ReviewType reviewType;

    // Trạng thái của đánh giá
    private ReviewStatus status;

    // ID của lịch hẹn liên quan đến đánh giá
    private UUID appointmentId;

    // ID của dịch vụ sửa chữa / bảo dưỡng liên quan đến đánh giá
    private UUID serviceId;

    // ID của xe (VIN) liên quan đến đánh giá
    private UUID dealershipId;

    // ID của thợ sửa xe được chỉ định cho đánh giá, có thể null nếu chưa được chỉ định
    private Short serviceRating;

    // Lời đánh giá của khách hàng về dịch vụ
    private String serviceComment;

    // ID của thợ sửa xe được chỉ định cho đánh giá, có thể null nếu chưa được chỉ định
    private UUID mechanicId;

    // Đánh giá của khách hàng về chất lượng phục vụ cửa thợ sửa xe
    private Short mechanicRating;

    // Lời đánh giá của khách hàng về chất lượng phục vụ cửa thợ sửa xe
    private String mechanicComment;

    // Thời điểm token đánh giá hết hạn, sau thời điểm này khách hàng sẽ không thể đánh giá nữa
    private Instant tokenExpiresAt;

    // Thời điểm khách hàng gửi đánh giá
    private Instant submittedAt;

    // Thời điểm tạo đánh giá
    private Instant createdAt;
}
