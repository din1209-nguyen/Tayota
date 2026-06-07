package com.tayota.operationservice.controller.workorder;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.request.workorder.AssignMechanicRequest;
import com.tayota.operationservice.dto.request.workorder.CreateServiceItemRequest;
import com.tayota.operationservice.dto.request.workorder.CreateWalkInServiceTicketRequest;
import com.tayota.operationservice.dto.request.workorder.RejectServiceTicketRequest;
import com.tayota.operationservice.dto.response.car.AccessoryResponseDTO;
import com.tayota.operationservice.dto.response.workorder.ServiceInvoiceResponse;
import com.tayota.operationservice.dto.response.workorder.ServiceTicketDetailResponse;
import com.tayota.operationservice.dto.response.workorder.ServiceTicketSummaryResponse;
import com.tayota.operationservice.service.workorder.WorkOrderService;
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

    @GetMapping("/user/my")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<List<ServiceTicketSummaryResponse>> getUserServiceTickets() {
        List<ServiceTicketSummaryResponse> response = workOrderService.getUserServiceTickets();

        return ApiResponse.success(200, "Lấy danh sách dịch vụ của khách hàng thành công!", response);
    }

    @GetMapping("/user/{serviceTicketId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<ServiceTicketDetailResponse> getUserServiceTicketDetail(
            @PathVariable UUID serviceTicketId
    ) {
        ServiceTicketDetailResponse response = workOrderService.getUserServiceTicketDetail(serviceTicketId);

        return ApiResponse.success(200, "Lấy chi tiết dịch vụ của khách hàng thành công!", response);
    }

    @GetMapping("/mechanic/my")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<List<ServiceTicketSummaryResponse>> getMyServiceTickets() {
        List<ServiceTicketSummaryResponse> response = workOrderService.getMyServiceTickets();

        return ApiResponse.success(200, "Lấy danh sách phiếu dịch vụ thành công!", response);
    }

    @GetMapping("/advisor")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<List<ServiceTicketSummaryResponse>> getAdvisorServiceTickets(
            @RequestParam(required = false) String status
    ) {
        List<ServiceTicketSummaryResponse> response = workOrderService.getAdvisorServiceTickets(status);

        return ApiResponse.success(200, "Lấy danh sách phiếu dịch vụ của đại lý thành công!", response);
    }

    @GetMapping("/advisor/{serviceTicketId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<ServiceTicketDetailResponse> getAdvisorServiceTicketDetail(
            @PathVariable UUID serviceTicketId
    ) {
        ServiceTicketDetailResponse response = workOrderService.getAdvisorServiceTicketDetail(serviceTicketId);

        return ApiResponse.success(200, "Lấy chi tiết phiếu dịch vụ thành công!", response);
    }

    @PostMapping("/advisor/walk-in")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<ServiceTicketSummaryResponse> createWalkInServiceTicket(
            @Valid @RequestBody CreateWalkInServiceTicketRequest request
    ) {
        ServiceTicketSummaryResponse response = workOrderService.createWalkInServiceTicket(request);

        return ApiResponse.success(201, "Tạo phiếu dịch vụ trực tiếp thành công!", response);
    }

    @PatchMapping("/advisor/{serviceTicketId}/assign-mechanic")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<ServiceTicketSummaryResponse> assignMechanic(
            @PathVariable UUID serviceTicketId,
            @Valid @RequestBody AssignMechanicRequest request
    ) {
        ServiceTicketSummaryResponse response = workOrderService.assignMechanic(serviceTicketId, request);

        return ApiResponse.success(200, "Phân công lại kỹ thuật viên thành công!", response);
    }

    @GetMapping("/{serviceTicketId}/invoice")
    @PreAuthorize("hasRole('MECHANIC') or hasRole('SERVICE_ADVISOR') or hasRole('USER')")
    public ApiResponse<ServiceInvoiceResponse> getServiceInvoice(@PathVariable UUID serviceTicketId) {
        ServiceInvoiceResponse response = workOrderService.getServiceInvoice(serviceTicketId);

        return ApiResponse.success(200, "Lấy phiếu thu thành công!", response);
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

    @PatchMapping("/mechanic/{serviceTicketId}/reject")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<ServiceTicketSummaryResponse> rejectServiceTicket(
            @PathVariable UUID serviceTicketId,
            @Valid @RequestBody RejectServiceTicketRequest request
    ) {
        ServiceTicketSummaryResponse response = workOrderService.rejectServiceTicket(serviceTicketId, request);

        return ApiResponse.success(200, "Đã từ chối tiếp nhận phiếu dịch vụ!", response);
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

    @PatchMapping("/mechanic/{serviceTicketId}/items/{itemId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<ServiceTicketDetailResponse> updateServiceItem(
            @PathVariable UUID serviceTicketId,
            @PathVariable UUID itemId,
            @Valid @RequestBody CreateServiceItemRequest request
    ) {
        ServiceTicketDetailResponse response = workOrderService.updateServiceItem(serviceTicketId, itemId, request);

        return ApiResponse.success(200, "Cập nhật hạng mục dịch vụ thành công!", response);
    }

    @DeleteMapping("/mechanic/{serviceTicketId}/items/{itemId}")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<ServiceTicketDetailResponse> deleteServiceItem(
            @PathVariable UUID serviceTicketId,
            @PathVariable UUID itemId
    ) {
        ServiceTicketDetailResponse response = workOrderService.deleteServiceItem(serviceTicketId, itemId);

        return ApiResponse.success(200, "Xóa hạng mục dịch vụ thành công!", response);
    }

    @GetMapping("/mechanic/{serviceTicketId}/recommended-accessories")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<List<AccessoryResponseDTO>> getRecommendedAccessories(@PathVariable UUID serviceTicketId) {
        List<AccessoryResponseDTO> response = workOrderService.getRecommendedAccessories(serviceTicketId);

        return ApiResponse.success(200, "Lấy phụ tùng phù hợp thành công!", response);
    }

    @PatchMapping("/mechanic/{serviceTicketId}/complete")
    @PreAuthorize("hasRole('MECHANIC')")
    public ApiResponse<ServiceTicketDetailResponse> completeServiceTicket(
            @PathVariable UUID serviceTicketId
    ) {
        ServiceTicketDetailResponse response = workOrderService.completeServiceTicket(serviceTicketId);

        return ApiResponse.success(200, "Hoàn thành phiếu dịch vụ thành công!", response);
    }
}
