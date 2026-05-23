package com.tayota.userservice.controller;

import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.userservice.dto.Request.workorder.CreateServiceItemRequest;
import com.tayota.userservice.dto.Response.workorder.ServiceTicketDetailResponse;
import com.tayota.userservice.dto.Response.workorder.ServiceTicketSummaryResponse;
import com.tayota.userservice.service.workorder.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workorders")
@RequiredArgsConstructor
public class WorkOrderController {
    private final WorkOrderService workOrderService;

    @GetMapping("/mechanic/my")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<List<ServiceTicketSummaryResponse>> getMyServiceTickets() {
        List<ServiceTicketSummaryResponse> response = workOrderService.getMyServiceTickets();

        return ApiResponse.success(200, "Lấy danh sách phiếu dịch vụ thành công!", response);
    }

    @GetMapping("/mechanic/{serviceTicketId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<ServiceTicketDetailResponse> getServiceTicketDetail(
            @PathVariable UUID serviceTicketId
    ) {
        ServiceTicketDetailResponse response = workOrderService.getServiceTicketDetail(serviceTicketId);

        return ApiResponse.success(200, "Lấy chi tiết phiếu dịch vụ thành công!", response);
    }

    @PatchMapping("/mechanic/{serviceTicketId}/receive")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<ServiceTicketSummaryResponse> receiveServiceTicket(
            @PathVariable UUID serviceTicketId
    ) {
        ServiceTicketSummaryResponse response = workOrderService.receiveServiceTicket(serviceTicketId);

        return ApiResponse.success(200, "Tiếp nhận phiếu dịch vụ thành công!", response);
    }

    @PatchMapping("/mechanic/{serviceTicketId}/start")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<ServiceTicketSummaryResponse> startServiceTicket(
            @PathVariable UUID serviceTicketId
    ) {
        ServiceTicketSummaryResponse response = workOrderService.startServiceTicket(serviceTicketId);

        return ApiResponse.success(200, "Bắt đầu sửa phiếu dịch vụ thành công!", response);
    }

    @PostMapping("/mechanic/{serviceTicketId}/items")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<ServiceTicketDetailResponse> addServiceItem(
            @PathVariable UUID serviceTicketId,
            @Valid @RequestBody CreateServiceItemRequest request
    ) {
        ServiceTicketDetailResponse response = workOrderService.addServiceItem(serviceTicketId, request);

        return ApiResponse.success(201, "Thêm hạng mục dịch vụ thành công!", response);
    }
}
