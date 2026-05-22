package com.tayota.userservice.controller;


import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.commoncore.exception.CustomException;
import com.tayota.userservice.dto.Request.CreateAppointmentHolidayRequest;
import com.tayota.userservice.dto.Request.CreateServiceAppointmentRequest;
import com.tayota.userservice.dto.Request.CreateServiceTimeSlotRequest;
import com.tayota.userservice.dto.Request.CreateTestDriveAppointmentRequest;
import com.tayota.userservice.dto.Request.UpdateAppointmentHolidayRequest;
import com.tayota.userservice.dto.Request.UpdateAppointmentRequest;
import com.tayota.userservice.dto.Request.UpdateServiceTimeSlotRequest;
import com.tayota.userservice.dto.Response.AppointmentAvailableSlotsResponse;
import com.tayota.userservice.dto.Response.AppointmentCreatedResponse;
import com.tayota.userservice.dto.Response.AppointmentHolidayResponse;
import com.tayota.userservice.dto.Response.AppointmentManagementDetailResponse;
import com.tayota.userservice.dto.Response.MyAppointmentDetailResponse;
import com.tayota.userservice.dto.Response.ServiceTimeSlotResponse;
import com.tayota.userservice.enums.AppointmentType;
import com.tayota.userservice.service.AppointmentScheduleService;
import com.tayota.userservice.service.AppointmentService;
import com.tayota.userservice.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final AppointmentScheduleService appointmentScheduleService;

    //======================== ENDPOINTS DÀNH CHO END-USER =============================

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

    // Dùng cho frontend lấy các khung giờ có thể đặt theo đại lý, loại lịch và ngày hẹn.
    @GetMapping("/available-slots")
    public ApiResponse<AppointmentAvailableSlotsResponse> getAvailableSlots(
            @RequestParam UUID dealershipId,
            @RequestParam AppointmentType appointmentType,
            @RequestParam LocalDate appointmentDate
    ) {
        AppointmentAvailableSlotsResponse response = appointmentScheduleService.getAvailableSlots(
                dealershipId,
                appointmentType,
                appointmentDate
        );

        return ApiResponse.success(200, "Lấy danh sách khung giờ thành công!", response);
    }

    // ========================= SERVICE DÀNH CHO SERVICE ADVISOR =============================
    // Dùng cho cố vấn dịch vụ xem appointment của đại lý mình.
    @GetMapping("/advisor")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<List<AppointmentCreatedResponse>> getAppointmentsForServiceAdvisor(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(defaultValue = "PENDING") String status
    ) {
        UUID serviceAdvisorId = parseRequiredUserId(userIdHeader, "Vui lòng đăng nhập để xem lịch hẹn");

        List<AppointmentCreatedResponse> response =
                appointmentService.getAppointmentsForServiceAdvisor(status, serviceAdvisorId);

        return ApiResponse.success(200, "Lấy danh sách lịch hẹn của đại lý thành công!", response);
    }

    // Dùng cho cố vấn dịch vụ xem chi tiết lịch hẹn của đại lý mình.
    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<AppointmentManagementDetailResponse> getAppointmentDetailForManagement(
            @PathVariable UUID appointmentId
    ) {
        AppointmentManagementDetailResponse response = appointmentService.getAppointmentDetailForManagement(appointmentId);

        return ApiResponse.success(200, "Lấy chi tiết lịch hẹn thành công!", response);
    }

    // Dùng cho cố vấn dịch vụ cập nhật lịch hẹn của đại lý mình, admin có thể cập nhật toàn bộ để hỗ trợ hệ thống.
    @PatchMapping("advisor/{appointmentId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<AppointmentManagementDetailResponse> updateAppointmentForManagement(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody UpdateAppointmentRequest request
    ) {
        AppointmentManagementDetailResponse response = appointmentService.updateAppointmentForManagement(appointmentId, request);

        return ApiResponse.success(200, "Cập nhật lịch hẹn thành công!", response);
    }

    // Dùng cho cố vấn dịch vụ quản lý khung giờ làm việc của đại lý mình
    @GetMapping("/advisor/time-slots")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<List<ServiceTimeSlotResponse>> getMyDealershipTimeSlots() {
        List<ServiceTimeSlotResponse> response = appointmentScheduleService.getMyDealershipTimeSlots();

        return ApiResponse.success(200, "Lấy danh sách khung giờ thành công!", response);
    }

    // Dùng cho cố vấn dịch vụ tạo khung giờ làm việc của đại lý mình, admin có thể tạo toàn bộ để hỗ trợ hệ thống.
    @PostMapping("/advisor/time-slots")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<ServiceTimeSlotResponse> createTimeSlot(
            @Valid @RequestBody CreateServiceTimeSlotRequest request
    ) {
        ServiceTimeSlotResponse response = appointmentScheduleService.createTimeSlot(request);

        return ApiResponse.success(201, "Tạo khung giờ thành công!", response);
    }

    // Dùng cho cố vấn dịch vụ cập nhật khung giờ làm việc của đại lý mình, admin có thể cập nhật toàn bộ để hỗ trợ hệ thống.
    @PatchMapping("/advisor/time-slots/{slotId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<ServiceTimeSlotResponse> updateTimeSlot(
            @PathVariable UUID slotId,
            @Valid @RequestBody UpdateServiceTimeSlotRequest request
    ) {
        ServiceTimeSlotResponse response = appointmentScheduleService.updateTimeSlot(slotId, request);

        return ApiResponse.success(200, "Cập nhật khung giờ thành công!", response);
    }

    // Dùng cho cố vấn dịch vụ xóa khung giờ làm việc của đại lý mình, admin có thể xóa toàn bộ để hỗ trợ hệ thống.
    @DeleteMapping("/advisor/time-slots/{slotId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<Void> deleteTimeSlot(@PathVariable UUID slotId) {
        appointmentScheduleService.deleteTimeSlot(slotId);

        return ApiResponse.success(200, "Xóa khung giờ thành công!", null);
    }

    // Dùng cho cố vấn dịch vụ quản lý ngày nghỉ của đại lý mình, admin có thể xem toàn bộ để hỗ trợ hệ thống.
    @GetMapping("/advisor/holidays")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<List<AppointmentHolidayResponse>> getMyDealershipHolidays() {
        List<AppointmentHolidayResponse> response = appointmentScheduleService.getMyDealershipHolidays();

        return ApiResponse.success(200, "Lấy danh sách ngày nghỉ thành công!", response);
    }

    // Dùng cho cố vấn dịch vụ tạo ngày nghỉ của đại lý mình, admin có thể tạo toàn bộ để hỗ trợ hệ thống.
    @PostMapping("/advisor/holidays")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<AppointmentHolidayResponse> createHoliday(
            @Valid @RequestBody CreateAppointmentHolidayRequest request
    ) {
        AppointmentHolidayResponse response = appointmentScheduleService.createHoliday(request);

        return ApiResponse.success(201, "Tạo ngày nghỉ thành công!", response);
    }

    // Dùng cho cố vấn dịch vụ cập nhật ngày nghỉ của đại lý mình, admin có thể cập nhật toàn bộ để hỗ trợ hệ thống.
    @PatchMapping("/advisor/holidays/{holidayId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<AppointmentHolidayResponse> updateHoliday(
            @PathVariable UUID holidayId,
            @Valid @RequestBody UpdateAppointmentHolidayRequest request
    ) {
        AppointmentHolidayResponse response = appointmentScheduleService.updateHoliday(holidayId, request);

        return ApiResponse.success(200, "Cập nhật ngày nghỉ thành công!", response);
    }

    // Dùng cho cố vấn dịch vụ xóa ngày nghỉ của đại lý mình, admin có thể xóa toàn bộ để hỗ trợ hệ thống.
    @DeleteMapping("/advisor/holidays/{holidayId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<Void> deleteHoliday(@PathVariable UUID holidayId) {
        appointmentScheduleService.deleteHoliday(holidayId);

        return ApiResponse.success(200, "Xóa ngày nghỉ thành công!", null);
    }

    // ========================= CÁC HÀM TIỆN ÍCH CHUNG =============================

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
