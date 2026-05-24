package com.tayota.userservice.dto.Request.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

// Dùng để nhận dữ liệu khi khách hàng tạo đánh giá dịch vụ và thợ sau khi hoàn thành lịch hẹn
@Getter
public class CreateCustomerReviewRequest {
    // ID của lịch hẹn mà đánh giá này liên quan đến, bắt buộc phải có để xác định lịch hẹn nào đang được đánh giá
    @NotNull(message = "Vui lòng chọn số sao đánh giá dịch vụ")
    @Min(value = 1, message = "Đánh giá dịch vụ phải từ 1 đến 5 sao")
    @Max(value = 5, message = "Đánh giá dịch vụ phải từ 1 đến 5 sao")
    private Short serviceRating;

    // Nhận xét của khách hàng về dịch vụ, có thể để trống nhưng không được vượt quá 1000 ký tự nếu có nhập
    @Size(max = 1000, message = "Nhận xét dịch vụ không được vượt quá 1000 ký tự")
    private String serviceComment;

    // Đánh giá của khách hàng về thợ sửa xe, bắt buộc phải có để đánh giá thợ sau khi hoàn thành lịch hẹn
    @Min(value = 1, message = "Đánh giá thợ phải từ 1 đến 5 sao")
    @Max(value = 5, message = "Đánh giá thợ phải từ 1 đến 5 sao")
    private Short mechanicRating;

    // Nhận xét của khách hàng về thợ sửa xe, có thể để trống nhưng không được vượt quá 1000 ký tự nếu có nhập
    @Size(max = 1000, message = "Nhận xét thợ không được vượt quá 1000 ký tự")
    private String mechanicComment;
}
