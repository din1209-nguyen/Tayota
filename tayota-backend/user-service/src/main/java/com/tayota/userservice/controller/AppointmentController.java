package com.tayota.userservice.controller;


import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.dto.Request.CreateServiceAppointmentRequest;
import com.tayota.userservice.dto.Request.CreateTestDriveAppointmentRequest;
import com.tayota.userservice.dto.Request.UpdateAppointmentRequest;
import com.tayota.userservice.dto.Response.AppointmentCreatedResponse;
import com.tayota.userservice.dto.Response.AppointmentManagementDetailResponse;
import com.tayota.userservice.dto.Response.MyAppointmentDetailResponse;
import com.tayota.userservice.service.AppointmentService;
import com.tayota.userservice.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    // Dùng cho quản lý/admin xem tất cả appointment, có thể lọc theo trạng thái (mặc định là PENDING)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ApiResponse<List<AppointmentCreatedResponse>> getAppointmentsForManagement(
            @RequestParam(defaultValue = "PENDING") String status
    ) {
        List<AppointmentCreatedResponse> response = appointmentService.getAppointmentsForManagement(status);

        return ApiResponse.success(200, "Lấy danh sách lịch hẹn thành công!", response);
    }

    // Dùng cho khách hàng chưa đăng nhập đặt lịch lái thử
    @PostMapping("/test-drive/guest")
    public ApiResponse<AppointmentCreatedResponse> createGuestTestDriveAppointment(
            @Valid @RequestBody CreateTestDriveAppointmentRequest request,
            HttpServletRequest servletRequest
    ) {
        AppointmentCreatedResponse response = appointmentService.createTestDriveAppointment(request, null, IpUtil.getClientIp(servletRequest));

        return ApiResponse.success(201, "Tạo lịch lái thử thành công, vui lòng chờ xác nhận!", response);
    }

    // Dùng cho khách hàng đã đăng nhập đặt lịch lái thử
    @PostMapping("/test-drive")
    public ApiResponse<AppointmentCreatedResponse> createUserTestDriveAppointment(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody CreateTestDriveAppointmentRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID userId = parseRequiredUserId(userIdHeader, "Vui lòng đăng nhập để đặt lịch lái thử");

       AppointmentCreatedResponse response = appointmentService.createTestDriveAppointment(request, userId, IpUtil.getClientIp(servletRequest));

        return ApiResponse.success(201, "Tạo lịch lái thử thành công, vui lòng chờ xác nhận!", response);
    }

    // Dùng cho khách hàng chưa đăng nhập đặt lịch dịch vụ, bắt buộc phải có thông tin về số VIN xe
    @PostMapping("/service/guest")
    public ApiResponse<AppointmentCreatedResponse> createGuestServiceAppointment(
            @Valid @RequestBody CreateServiceAppointmentRequest request,
            HttpServletRequest servletRequest
    ) {
        AppointmentCreatedResponse response = appointmentService.createServiceAppointment(
                request,
                null,
                IpUtil.getClientIp(servletRequest)
        );

        return ApiResponse.success(201, "Tạo lịch dịch vụ thành công, vui lòng chờ xác nhận!", response);
    }

    // Dùng cho khách hàng đã đăng nhập đặt lịch dịch vụ, bắt buộc phải có thông tin về số VIN xe
    @PostMapping("/service")
    public ApiResponse<AppointmentCreatedResponse> createServiceAppointment(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody CreateServiceAppointmentRequest request,
            HttpServletRequest servletRequest
    ) {
        UUID userId = parseRequiredUserId(userIdHeader, "Vui lòng đăng nhập để đặt lịch sửa chữa/bảo dưỡng");

        AppointmentCreatedResponse response = appointmentService.createServiceAppointment(request, userId, IpUtil.getClientIp(servletRequest));

        return ApiResponse.success(201, "Tạo lịch dịch vụ thành công, vui lòng chờ xác nhận!", response);
    }

    // Dùng cho khách hàng đã đăng nhập xem danh sách lịch hẹn của chính mình, có thể bao gồm cả lịch lái thử và lịch dịch vụ
    @GetMapping("/my")
    public ApiResponse<List<AppointmentCreatedResponse>> getMyAppointments(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID userId = parseRequiredUserId(userIdHeader, "Vui lòng đăng nhập để xem lịch hẹn");

        List<AppointmentCreatedResponse> response = appointmentService.getMyAppointments(userId);

        return ApiResponse.success(200, "Lấy danh sách lịch hẹn thành công!", response);
    }

    // Dùng cho khách hàng đã đăng nhập xem chi tiết một lịch hẹn của chính mình, có thể bao gồm cả lịch lái thử và lịch dịch vụ
    @GetMapping("/my/{appointmentId}")
    public ApiResponse<MyAppointmentDetailResponse> getMyAppointmentDetail(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @PathVariable UUID appointmentId
    ) {
        UUID userId = parseRequiredUserId(userIdHeader, "Vui lòng đăng nhập để xem chi tiết lịch hẹn");

        MyAppointmentDetailResponse response = appointmentService.getMyAppointmentDetail(appointmentId, userId);

        return ApiResponse.success(200, "Lấy chi tiết lịch hẹn thành công!", response);
    }

    // Dùng cho quản lý/admin xem chi tiết một lịch hẹn bất kỳ, có thể bao gồm cả lịch lái thử và lịch dịch vụ
    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ApiResponse<AppointmentManagementDetailResponse> getAppointmentDetailForManagement(
            @PathVariable UUID appointmentId
    ) {
        AppointmentManagementDetailResponse response = appointmentService.getAppointmentDetailForManagement(appointmentId);

        return ApiResponse.success(200, "Lấy chi tiết lịch hẹn thành công!", response);
    }

    // Dùng cho quản lý/admin cập nhật thông tin một lịch hẹn bất kỳ, có thể bao gồm cả lịch lái thử và lịch dịch vụ, nhưng không được phép thay đổi userId (người đặt lịch)
    @PatchMapping("/{appointmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ApiResponse<AppointmentManagementDetailResponse> updateAppointmentForManagement(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody UpdateAppointmentRequest request
    ) {
        AppointmentManagementDetailResponse response = appointmentService.updateAppointmentForManagement(appointmentId, request);

        return ApiResponse.success(200, "Cập nhật lịch hẹn thành công!", response);
    }

    // Hàm tiện ích để parse và validate userId từ header, nếu không hợp lệ sẽ ném ra lỗi 401 với thông điệp tùy chỉnh
    private UUID parseRequiredUserId(String userIdHeader, String errorMessage) {
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            throw new CustomException(401, errorMessage);
        }

        try {
            return UUID.fromString(userIdHeader.trim());
        } catch (IllegalArgumentException exception) {
            throw new CustomException(401, "Thông tin người dùng không hợp lệ");
        }

    }
}
