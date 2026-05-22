package com.tayota.userservice.controller;


import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.dto.Request.CreateServiceAppointmentRequest;
import com.tayota.userservice.dto.Request.CreateTestDriveAppointmentRequest;
import com.tayota.userservice.dto.Response.AppointmentCreatedResponse;
import com.tayota.userservice.dto.Response.AppointmentResponse;
import com.tayota.userservice.service.AppointmentService;
import com.tayota.userservice.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping("/test-drive/guest")
    public ApiResponse<AppointmentCreatedResponse> createGuestTestDriveAppointment(
            @Valid @RequestBody CreateTestDriveAppointmentRequest request,
            HttpServletRequest servletRequest
    ) {
        AppointmentCreatedResponse response = appointmentService.createTestDriveAppointment(request, null, IpUtil.getClientIp(servletRequest));

        return ApiResponse.success(201, "Tạo lịch lái thử thành công, vui lòng chờ xác nhận!", response);
    }

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

    @GetMapping("/my")
    public ApiResponse<List<AppointmentCreatedResponse>> getMyAppointments(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        UUID userId = parseRequiredUserId(userIdHeader, "Vui lòng đăng nhập để xem lịch hẹn");

        List<AppointmentCreatedResponse> response = appointmentService.getMyAppointments(userId);

        return ApiResponse.success(200, "Lấy danh sách lịch hẹn thành công!", response);
    }

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