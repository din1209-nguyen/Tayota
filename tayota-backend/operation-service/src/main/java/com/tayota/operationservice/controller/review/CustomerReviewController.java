package com.tayota.operationservice.controller.review;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.request.review.CreateCustomerReviewRequest;
import com.tayota.operationservice.dto.response.review.AdvisorReviewSummaryResponse;
import com.tayota.operationservice.dto.response.review.CustomerReviewResponse;
import com.tayota.operationservice.dto.response.review.MechanicReviewSummaryResponse;
import com.tayota.operationservice.service.review.CustomerReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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

    @GetMapping("/mechanic/my")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<List<CustomerReviewResponse>> getMyMechanicReviews() {
        List<CustomerReviewResponse> response = customerReviewService.getMyMechanicReviews();

        return ApiResponse.success(200, "Lấy danh sách đánh giá kỹ thuật viên thành công!", response);
    }

    @GetMapping("/mechanic/my/summary")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<MechanicReviewSummaryResponse> getMyMechanicReviewSummary() {
        MechanicReviewSummaryResponse response = customerReviewService.getMyMechanicReviewSummary();

        return ApiResponse.success(200, "Lấy thống kê đánh giá kỹ thuật viên thành công!", response);
    }

    @GetMapping("/advisor/summary")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<AdvisorReviewSummaryResponse> getAdvisorReviewSummary(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        AdvisorReviewSummaryResponse response = customerReviewService.getAdvisorReviewSummary(from, to);

        return ApiResponse.success(200, "Lấy thống kê đánh giá đại lý thành công!", response);
    }
}
