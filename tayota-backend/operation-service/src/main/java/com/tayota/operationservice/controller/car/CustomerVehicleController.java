package com.tayota.operationservice.controller.car;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.request.car.AssignCustomerVehicleRequest;
import com.tayota.operationservice.dto.response.car.CustomerVehicleResponse;
import com.tayota.operationservice.service.car.CustomerVehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customer-vehicles")
@RequiredArgsConstructor
public class CustomerVehicleController {
    private final CustomerVehicleService customerVehicleService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<List<CustomerVehicleResponse>> getMyVehicles() {
        List<CustomerVehicleResponse> response = customerVehicleService.getMyVehicles();

        return ApiResponse.success(200, "Lấy danh sách xe của tôi thành công!", response);
    }

    @PostMapping("/advisor/assign")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<CustomerVehicleResponse> assignVehicleToCustomer(
            @Valid @RequestBody AssignCustomerVehicleRequest request
    ) {
        CustomerVehicleResponse response = customerVehicleService.assignVehicleToCustomer(request);

        return ApiResponse.success(200, "Gán xe cho khách hàng thành công!", response);
    }

    @GetMapping("/advisor/customer/{userId}")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<List<CustomerVehicleResponse>> getCustomerVehicles(@PathVariable UUID userId) {
        List<CustomerVehicleResponse> response = customerVehicleService.getCustomerVehicles(userId);

        return ApiResponse.success(200, "Lấy danh sách xe của khách hàng thành công!", response);
    }

    @PatchMapping("/advisor/{vinId}/inactive")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<Void> removeVehicleFromCustomer(@PathVariable String vinId) {
        customerVehicleService.removeVehicleFromCustomer(vinId);

        return ApiResponse.success(200, "Gỡ xe khỏi khách hàng thành công!", null);
    }
}
