package com.tayota.operationservice.controller;

import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.operationservice.dto.Request.review.CreateCustomerReviewRequest;
import com.tayota.operationservice.dto.Response.review.CustomerReviewResponse;
import com.tayota.operationservice.service.review.CustomerReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class CustomerReviewController {
    private final CustomerReviewService customerReviewService;

    // Đánh giá lịch hẹn sau khi hoàn thành.
    // Dành cho cả khách hàng đăng nhập và guest.
    @PostMapping("/appointments/{appointmentId}")
    public ApiResponse<CustomerReviewResponse> reviewAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody CreateCustomerReviewRequest request
    ) {
        CustomerReviewResponse response = customerReviewService.reviewAppointment(appointmentId, request);

        return ApiResponse.success(201, "Đánh giá lịch hẹn thành công!", response);
    }

    // Lấy thông tin đánh giá đang ở trạng thái pending để hiển thị trên trang đánh giá.
    // FE sẽ gọi API này khi khách hàng click vào link đánh giá trong email hoặc notification sau khi lịch hẹn hoàn thành.
    @GetMapping("/token/{token}")
    public ApiResponse<CustomerReviewResponse> getReviewByToken(
            @PathVariable String token
    ) {
        CustomerReviewResponse response = customerReviewService.getReviewByToken(token);

        return ApiResponse.success(200, "Lấy thông tin đánh giá thành công!", response);
    }

    // Đánh giá lịch hẹn thông qua token.
    // Sau khi khách hàng hoàn thành đánh giá trên trang đánh giá, FE sẽ gửi request đến API này để cập nhật đánh giá vào hệ thống.
    @PatchMapping("/token/{token}")
    public ApiResponse<CustomerReviewResponse> submitReviewByToken(
            @PathVariable String token,
            @Valid @RequestBody CreateCustomerReviewRequest request
    ) {
        CustomerReviewResponse response = customerReviewService.submitReviewByToken(token, request);

        return ApiResponse.success(201, "Đánh giá thành công!", response);
    }

    // Đánh giá phiếu dịch vụ sau khi hoàn thành lịch hẹn.
    @PostMapping("/services/{serviceTicketId}")
    public ApiResponse<CustomerReviewResponse> reviewServiceTicket(
            @PathVariable UUID serviceTicketId,
            @Valid @RequestBody CreateCustomerReviewRequest request
    ) {
        CustomerReviewResponse response = customerReviewService.reviewServiceTicket(serviceTicketId, request);

        return ApiResponse.success(201, "Đánh giá dịch vụ thành công!", response);
    }

    // Lấy danh sách đánh giá của khách hàng cho tất cả lịch hẹn và phiếu dịch vụ đã đánh giá. FE sẽ hiển thị trong trang cá nhân của khách hàng.
    @GetMapping("/my")
    public ApiResponse<List<CustomerReviewResponse>> getMyReviews() {
        List<CustomerReviewResponse> response = customerReviewService.getMyReviews();

        return ApiResponse.success(200, "Lấy danh sách đánh giá thành công!", response);
    }
}
