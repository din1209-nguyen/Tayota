package com.tayota.operationservice.controller.appointment;


import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.request.appointment.CreateAppointmentHolidayRequest;
import com.tayota.operationservice.dto.request.appointment.CreateServiceAppointmentRequest;
import com.tayota.operationservice.dto.request.appointment.CreateServiceTimeSlotRequest;
import com.tayota.operationservice.dto.request.appointment.CreateTestDriveAppointmentRequest;
import com.tayota.operationservice.dto.request.appointment.UpdateAppointmentHolidayRequest;
import com.tayota.operationservice.dto.request.appointment.UpdateAppointmentRequest;
import com.tayota.operationservice.dto.request.appointment.UpdateServiceTimeSlotRequest;
import com.tayota.operationservice.dto.request.workorder.CheckInServiceAppointmentRequest;
import com.tayota.operationservice.dto.response.appointment.AppointmentAvailableSlotsResponse;
import com.tayota.operationservice.dto.response.appointment.AppointmentCreatedResponse;
import com.tayota.operationservice.dto.response.appointment.AppointmentHolidayResponse;
import com.tayota.operationservice.dto.response.appointment.AppointmentManagementDetailResponse;
import com.tayota.operationservice.dto.response.appointment.MyAppointmentDetailResponse;
import com.tayota.operationservice.dto.response.appointment.ServiceTimeSlotResponse;
import com.tayota.operationservice.dto.response.workorder.CheckInServiceAppointmentResponse;
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

    // Äáº·t lá»‹ch lÃ¡i thá»­ cho khÃ¡ch hÃ ng chÆ°a Ä‘Äƒng nháº­p
    @PostMapping("/test-drive/guest")
    public ApiResponse<AppointmentCreatedResponse> createGuestTestDriveAppointment(
            @Valid @RequestBody CreateTestDriveAppointmentRequest request,
            HttpServletRequest servletRequest
    ) {
        AppointmentCreatedResponse response = appointmentService.createTestDriveAppointment(request, null, IpUtil.getClientIp(servletRequest));

        return ApiResponse.success(201, "Táº¡o lá»‹ch lÃ¡i thá»­ thÃ nh cÃ´ng, vui lÃ²ng chá» xÃ¡c nháº­n!", response);
    }

    // Äáº·t lá»‹ch lÃ¡i thá»­ cho khÃ¡ch hÃ ng Ä‘Ã£ Ä‘Äƒng nháº­p
    @PostMapping("/test-drive")
    public ApiResponse<AppointmentCreatedResponse> createUserTestDriveAppointment(
            @Valid @RequestBody CreateTestDriveAppointmentRequest request,
            HttpServletRequest servletRequest
    ) {
       AppointmentCreatedResponse response = appointmentService.createUserTestDriveAppointment(request, IpUtil.getClientIp(servletRequest));

        return ApiResponse.success(201, "Táº¡o lá»‹ch lÃ¡i thá»­ thÃ nh cÃ´ng, vui lÃ²ng chá» xÃ¡c nháº­n!", response);
    }

    // Äáº·t lá»‹ch dá»‹ch vá»¥ cho khÃ¡ch hÃ ng chÆ°a Ä‘Äƒng nháº­p, báº¯t buá»™c pháº£i cÃ³ thÃ´ng tin vÃªÌ€ sÃ´Ì VIN
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

        return ApiResponse.success(201, "Táº¡o lá»‹ch dá»‹ch vá»¥ thÃ nh cÃ´ng, vui lÃ²ng chá» xÃ¡c nháº­n!", response);
    }

    // Äáº·t lá»‹ch dá»‹ch vá»¥ cho khÃ¡ch hÃ ng Ä‘Ã£ Ä‘Äƒng nháº­p, báº¯t buá»™c pháº£i cÃ³ thÃ´ng tin vá» sá»‘ VIN xe
    @PostMapping("/service")
    public ApiResponse<AppointmentCreatedResponse> createServiceAppointment(
            @Valid @RequestBody CreateServiceAppointmentRequest request,
            HttpServletRequest servletRequest
    ) {
        AppointmentCreatedResponse response = appointmentService.createUserServiceAppointment(request, IpUtil.getClientIp(servletRequest));

        return ApiResponse.success(201, "Táº¡o lá»‹ch dá»‹ch vá»¥ thÃ nh cÃ´ng, vui lÃ²ng chá» xÃ¡c nháº­n!", response);
    }

    // Láº¥y danh sÃ¡ch lá»‹ch háº¹n cá»§a khÃ¡ch hÃ ng
    @GetMapping("/my")
    public ApiResponse<List<AppointmentCreatedResponse>> getMyAppointments() {
        List<AppointmentCreatedResponse> response = appointmentService.getMyAppointments();

        return ApiResponse.success(200, "Láº¥y danh sÃ¡ch lá»‹ch háº¹n thÃ nh cÃ´ng!", response);
    }

    // Láº¥y chi tiáº¿t lá»‹ch háº¹n cá»§a khÃ¡ch hÃ ng
    @GetMapping("/my/{appointmentId}")
    public ApiResponse<MyAppointmentDetailResponse> getMyAppointmentDetail(
            @PathVariable UUID appointmentId
    ) {
        MyAppointmentDetailResponse response = appointmentService.getMyAppointmentDetail(appointmentId);

        return ApiResponse.success(200, "Láº¥y chi tiáº¿t lá»‹ch háº¹n thÃ nh cÃ´ng!", response);
    }

    // DÃ¹ng cho frontend láº¥y cÃ¡c khung giá» cÃ³ thá»ƒ Ä‘áº·t theo Ä‘áº¡i lÃ½, loáº¡i lá»‹ch vÃ  ngÃ y háº¹n.
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

        return ApiResponse.success(200, "Láº¥y danh sÃ¡ch khung giá» thÃ nh cÃ´ng!", response);
    }

    // DÃ¹ng cho cá»‘ váº¥n dá»‹ch vá»¥ xem appointment cá»§a Ä‘áº¡i lÃ½ mÃ¬nh.
    @GetMapping("/advisor")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<List<AppointmentCreatedResponse>> getAppointmentsForServiceAdvisor(
            @RequestParam(defaultValue = "PENDING") String status
    ) {
        List<AppointmentCreatedResponse> response =
                appointmentService.getAppointmentsForServiceAdvisor(status);

        return ApiResponse.success(200, "Láº¥y danh sÃ¡ch lá»‹ch háº¹n cá»§a Ä‘áº¡i lÃ½ thÃ nh cÃ´ng!", response);
    }

    // DÃ¹ng cho cá»‘ váº¥n dá»‹ch vá»¥ xem chi tiáº¿t lá»‹ch háº¹n cá»§a Ä‘áº¡i lÃ½ mÃ¬nh.
    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<AppointmentManagementDetailResponse> getAppointmentDetailForManagement(
            @PathVariable UUID appointmentId
    ) {
        AppointmentManagementDetailResponse response = appointmentService.getAppointmentDetailForManagement(appointmentId);

        return ApiResponse.success(200, "Láº¥y chi tiáº¿t lá»‹ch háº¹n thÃ nh cÃ´ng!", response);
    }

    // DÃ¹ng cho cá»‘ váº¥n dá»‹ch vá»¥ cáº­p nháº­t lá»‹ch háº¹n cá»§a Ä‘áº¡i lÃ½ mÃ¬nh, admin cÃ³ thá»ƒ cáº­p nháº­t toÃ n bá»™ Ä‘á»ƒ há»— trá»£ há»‡ thá»‘ng.
    @PatchMapping("advisor/{appointmentId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<AppointmentManagementDetailResponse> updateAppointmentForManagement(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody UpdateAppointmentRequest request
    ) {
        AppointmentManagementDetailResponse response = appointmentService.updateAppointmentForManagement(appointmentId, request);

        return ApiResponse.success(200, "Cáº­p nháº­t lá»‹ch háº¹n thÃ nh cÃ´ng!", response);
    }

    // DÃ¹ng cho cá»‘ váº¥n dá»‹ch vá»¥ xÃ¡c nháº­n khÃ¡ch Ä‘Ã£ Ä‘áº¿n Ä‘áº¡i lÃ½ cho lá»‹ch lÃ¡i thá»­.
    @PatchMapping("advisor/test-drive/{appointmentId}/check-in")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<AppointmentManagementDetailResponse> checkInTestDriveAppointment(
            @PathVariable UUID appointmentId
    ) {
        AppointmentManagementDetailResponse response = appointmentService.checkInTestDriveAppointment(appointmentId);

        return ApiResponse.success(200, "Check-in lá»‹ch lÃ¡i thá»­ thÃ nh cÃ´ng!", response);
    }

    // DÃ¹ng cho cá»‘ váº¥n dá»‹ch vá»¥ xÃ¡c nháº­n khÃ¡ch Ä‘Ã£ Ä‘áº¿n Ä‘áº¡i lÃ½ cho lá»‹ch dá»‹ch vá»¥, Ä‘á»“ng thá»i táº¡o phiáº¿u dá»‹ch vá»¥.
    @PatchMapping("advisor/service/{appointmentId}/check-in")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<CheckInServiceAppointmentResponse> checkInServiceAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody CheckInServiceAppointmentRequest request
    ) {
        CheckInServiceAppointmentResponse response = appointmentService.checkInServiceAppointment(appointmentId, request);

        return ApiResponse.success(200, "Check-in lá»‹ch dá»‹ch vá»¥ thÃ nh cÃ´ng!", response);
    }

    // DuÌ€ng cho cÃ´Ì vÃ¢Ìn diÌ£ch vuÌ£ quaÌ‰n lyÌ khung giÆ¡Ì€ laÌ€m viÃªÌ£c cuÌ‰a Ä‘aÌ£i lyÌ miÌ€nh
    @GetMapping("/advisor/time-slots")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<List<ServiceTimeSlotResponse>> getMyDealershipTimeSlots() {
        List<ServiceTimeSlotResponse> response = appointmentScheduleService.getMyDealershipTimeSlots();

        return ApiResponse.success(200, "Láº¥y danh sÃ¡ch khung giá» thÃ nh cÃ´ng!", response);
    }

    // DuÌ€ng cho cÃ´Ì vÃ¢Ìn diÌ£ch vuÌ£ taÌ£o khung giÆ¡Ì€ laÌ€m viÃªÌ£c cuÌ‰a Ä‘aÌ£i lyÌ miÌ€nh, admin coÌ thÃªÌ‰ taÌ£o toaÌ€n bÃ´Ì£ Ä‘ÃªÌ‰ hÃ´Ìƒ trÆ¡Ì£ hÃªÌ£ thÃ´Ìng.
    @PostMapping("/advisor/time-slots")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<ServiceTimeSlotResponse> createTimeSlot(
            @Valid @RequestBody CreateServiceTimeSlotRequest request
    ) {
        ServiceTimeSlotResponse response = appointmentScheduleService.createTimeSlot(request);

        return ApiResponse.success(201, "Táº¡o khung giá» thÃ nh cÃ´ng!", response);
    }

    // DuÌ€ng cho cÃ´Ì vÃ¢Ìn diÌ£ch vuÌ£ cÃ¢Ì£p nhÃ¢Ì£t khung giÆ¡Ì€ laÌ€m viÃªÌ£c cuÌ‰a Ä‘aÌ£i lyÌ miÌ€nh, admin coÌ thÃªÌ‰ cÃ¢Ì£p nhÃ¢Ì£t toaÌ€n bÃ´Ì£ Ä‘ÃªÌ‰ hÃ´Ìƒ trÆ¡Ì£ hÃªÌ£ thÃ´Ìng.
    @PatchMapping("/advisor/time-slots/{slotId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<ServiceTimeSlotResponse> updateTimeSlot(
            @PathVariable UUID slotId,
            @Valid @RequestBody UpdateServiceTimeSlotRequest request
    ) {
        ServiceTimeSlotResponse response = appointmentScheduleService.updateTimeSlot(slotId, request);

        return ApiResponse.success(200, "Cáº­p nháº­t khung giá» thÃ nh cÃ´ng!", response);
    }

    // DuÌ€ng cho cÃ´Ì vÃ¢Ìn diÌ£ch vuÌ£ xoÌa khung giÆ¡Ì€ laÌ€m viÃªÌ£c cuÌ‰a Ä‘aÌ£i lyÌ miÌ€nh, admin coÌ thÃªÌ‰ xoÌa toaÌ€n bÃ´Ì£ Ä‘ÃªÌ‰ hÃ´Ìƒ trÆ¡Ì£ hÃªÌ£ thÃ´Ìng.
    @DeleteMapping("/advisor/time-slots/{slotId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<Void> deleteTimeSlot(@PathVariable UUID slotId) {
        appointmentScheduleService.deleteTimeSlot(slotId);

        return ApiResponse.success(200, "XÃ³a khung giá» thÃ nh cÃ´ng!", null);
    }

    // DuÌ€ng cho cÃ´Ì vÃ¢Ìn diÌ£ch vuÌ£ quaÌ‰n lyÌ ngaÌ€y nghiÌ‰ cuÌ‰a Ä‘aÌ£i lyÌ miÌ€nh, admin coÌ thÃªÌ‰ xem toaÌ€n bÃ´Ì£ Ä‘ÃªÌ‰ hÃ´Ìƒ trÆ¡Ì£ hÃªÌ£ thÃ´Ìng.
    @GetMapping("/advisor/holidays")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<List<AppointmentHolidayResponse>> getMyDealershipHolidays() {
        List<AppointmentHolidayResponse> response = appointmentScheduleService.getMyDealershipHolidays();

        return ApiResponse.success(200, "Láº¥y danh sÃ¡ch ngÃ y nghá»‰ thÃ nh cÃ´ng!", response);
    }

    // DuÌ€ng cho cÃ´Ì vÃ¢Ìn diÌ£ch vuÌ£ taÌ£o ngaÌ€y nghiÌ‰ cuÌ‰a Ä‘aÌ£i lyÌ miÌ€nh, admin coÌ thÃªÌ‰ taÌ£o toaÌ€n bÃ´Ì£ Ä‘ÃªÌ‰ hÃ´Ìƒ trÆ¡Ì£ hÃªÌ£ thÃ´Ìng.
    @PostMapping("/advisor/holidays")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<AppointmentHolidayResponse> createHoliday(
            @Valid @RequestBody CreateAppointmentHolidayRequest request
    ) {
        AppointmentHolidayResponse response = appointmentScheduleService.createHoliday(request);

        return ApiResponse.success(201, "Táº¡o ngÃ y nghá»‰ thÃ nh cÃ´ng!", response);
    }

    // DuÌ€ng cho cÃ´Ì vÃ¢Ìn diÌ£ch vuÌ£ cÃ¢Ì£p nhÃ¢Ì£t ngaÌ€y nghiÌ‰ cuÌ‰a Ä‘aÌ£i lyÌ miÌ€nh, admin coÌ thÃªÌ‰ cÃ¢Ì£p nhÃ¢Ì£t toaÌ€n bÃ´Ì£ Ä‘ÃªÌ‰ hÃ´Ìƒ trÆ¡Ì£ hÃªÌ£ thÃ´Ìng.
    @PatchMapping("/advisor/holidays/{holidayId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<AppointmentHolidayResponse> updateHoliday(
            @PathVariable UUID holidayId,
            @Valid @RequestBody UpdateAppointmentHolidayRequest request
    ) {
        AppointmentHolidayResponse response = appointmentScheduleService.updateHoliday(holidayId, request);

        return ApiResponse.success(200, "Cáº­p nháº­t ngÃ y nghá»‰ thÃ nh cÃ´ng!", response);
    }

    // DuÌ€ng cho cÃ´Ì vÃ¢Ìn diÌ£ch vuÌ£ xoÌa ngaÌ€y nghiÌ‰ cuÌ‰a Ä‘aÌ£i lyÌ miÌ€nh, admin coÌ thÃªÌ‰ xoÌa toaÌ€n bÃ´Ì£ Ä‘ÃªÌ‰ hÃ´Ìƒ trÆ¡Ì£ hÃªÌ£ thÃ´Ìng.
    @DeleteMapping("/advisor/holidays/{holidayId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<Void> deleteHoliday(@PathVariable UUID holidayId) {
        appointmentScheduleService.deleteHoliday(holidayId);

        return ApiResponse.success(200, "XÃ³a ngÃ y nghá»‰ thÃ nh cÃ´ng!", null);
    }

}

