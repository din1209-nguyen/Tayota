package com.tayota.operationservice.controller;


import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.operationservice.dto.Request.appointment.CreateAppointmentHolidayRequest;
import com.tayota.operationservice.dto.Request.appointment.CreateServiceAppointmentRequest;
import com.tayota.operationservice.dto.Request.appointment.CreateServiceTimeSlotRequest;
import com.tayota.operationservice.dto.Request.appointment.CreateTestDriveAppointmentRequest;
import com.tayota.operationservice.dto.Request.appointment.UpdateAppointmentHolidayRequest;
import com.tayota.operationservice.dto.Request.appointment.UpdateAppointmentRequest;
import com.tayota.operationservice.dto.Request.appointment.UpdateServiceTimeSlotRequest;
import com.tayota.operationservice.dto.Request.workorder.CheckInServiceAppointmentRequest;
import com.tayota.operationservice.dto.Response.appointment.AppointmentAvailableSlotsResponse;
import com.tayota.operationservice.dto.Response.appointment.AppointmentCreatedResponse;
import com.tayota.operationservice.dto.Response.appointment.AppointmentHolidayResponse;
import com.tayota.operationservice.dto.Response.appointment.AppointmentManagementDetailResponse;
import com.tayota.operationservice.dto.Response.appointment.MyAppointmentDetailResponse;
import com.tayota.operationservice.dto.Response.appointment.ServiceTimeSlotResponse;
import com.tayota.operationservice.dto.Response.workorder.CheckInServiceAppointmentResponse;
import com.tayota.operationservice.enums.appointment.AppointmentType;
import com.tayota.operationservice.service.appointment.AppointmentScheduleService;
import com.tayota.operationservice.service.appointment.AppointmentService;
import com.tayota.operationservice.util.IpUtil;
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

    // Đặt lịch lái thử cho khách hàng chưa đăng nhập
    @PostMapping("/test-drive/guest")
    public ApiResponse<AppointmentCreatedResponse> createGuestTestDriveAppointment(
            @Valid @RequestBody CreateTestDriveAppointmentRequest request,
            HttpServletRequest servletRequest
    ) {
        AppointmentCreatedResponse response = appointmentService.createTestDriveAppointment(request, null, IpUtil.getClientIp(servletRequest));

        return ApiResponse.success(201, "Tạo lịch lái thử thành công, vui lòng chờ xác nhận!", response);
    }

    // Đặt lịch lái thử cho khách hàng đã đăng nhập
    @PostMapping("/test-drive")
    public ApiResponse<AppointmentCreatedResponse> createUserTestDriveAppointment(
            @Valid @RequestBody CreateTestDriveAppointmentRequest request,
            HttpServletRequest servletRequest
    ) {
       AppointmentCreatedResponse response = appointmentService.createUserTestDriveAppointment(request, IpUtil.getClientIp(servletRequest));

        return ApiResponse.success(201, "Tạo lịch lái thử thành công, vui lòng chờ xác nhận!", response);
    }

    // Đặt lịch dịch vụ cho khách hàng chưa đăng nhập, bắt buộc phải có thông tin về số VIN
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

    // Đặt lịch dịch vụ cho khách hàng đã đăng nhập, bắt buộc phải có thông tin về số VIN xe
    @PostMapping("/service")
    public ApiResponse<AppointmentCreatedResponse> createServiceAppointment(
            @Valid @RequestBody CreateServiceAppointmentRequest request,
            HttpServletRequest servletRequest
    ) {
        AppointmentCreatedResponse response = appointmentService.createUserServiceAppointment(request, IpUtil.getClientIp(servletRequest));

        return ApiResponse.success(201, "Tạo lịch dịch vụ thành công, vui lòng chờ xác nhận!", response);
    }

    // Lấy danh sách lịch hẹn của khách hàng
    @GetMapping("/my")
    public ApiResponse<List<AppointmentCreatedResponse>> getMyAppointments() {
        List<AppointmentCreatedResponse> response = appointmentService.getMyAppointments();

        return ApiResponse.success(200, "Lấy danh sách lịch hẹn thành công!", response);
    }

    // Lấy chi tiết lịch hẹn của khách hàng
    @GetMapping("/my/{appointmentId}")
    public ApiResponse<MyAppointmentDetailResponse> getMyAppointmentDetail(
            @PathVariable UUID appointmentId
    ) {
        MyAppointmentDetailResponse response = appointmentService.getMyAppointmentDetail(appointmentId);

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

    // Dùng cho cố vấn dịch vụ xem appointment của đại lý mình.
    @GetMapping("/advisor")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<List<AppointmentCreatedResponse>> getAppointmentsForServiceAdvisor(
            @RequestParam(defaultValue = "PENDING") String status
    ) {
        List<AppointmentCreatedResponse> response =
                appointmentService.getAppointmentsForServiceAdvisor(status);

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

    // Dùng cho cố vấn dịch vụ xác nhận khách đã đến đại lý cho lịch lái thử.
    @PatchMapping("advisor/test-drive/{appointmentId}/check-in")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<AppointmentManagementDetailResponse> checkInTestDriveAppointment(
            @PathVariable UUID appointmentId
    ) {
        AppointmentManagementDetailResponse response = appointmentService.checkInTestDriveAppointment(appointmentId);

        return ApiResponse.success(200, "Check-in lịch lái thử thành công!", response);
    }

    // Dùng cho cố vấn dịch vụ xác nhận khách đã đến đại lý cho lịch dịch vụ, đồng thời tạo phiếu dịch vụ.
    @PatchMapping("advisor/service/{appointmentId}/check-in")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<CheckInServiceAppointmentResponse> checkInServiceAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody CheckInServiceAppointmentRequest request
    ) {
        CheckInServiceAppointmentResponse response = appointmentService.checkInServiceAppointment(appointmentId, request);

        return ApiResponse.success(200, "Check-in lịch dịch vụ thành công!", response);
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

}
