package com.tayota.userservice.controller;

import com.tayota.commoncore.dto.ApiResponse;
import com.tayota.userservice.dto.Response.workorder.MechanicResponse;
import com.tayota.userservice.service.workorder.MechanicService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mechanics")
@RequiredArgsConstructor
public class MechanicController {
    private final MechanicService mechanicService;

    // Dùng cho cố vấn dịch vụ lấy danh sách thợ đang hoạt động của đại lý mình để gán vào phiếu dịch vụ.
    @GetMapping("/advisor/active")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ApiResponse<List<MechanicResponse>> getActiveMechanicsForMyDealership() {
        List<MechanicResponse> response = mechanicService.getActiveMechanicsForMyDealership();

        return ApiResponse.success(200, "Lấy danh sách thợ đang hoạt động thành công!", response);
    }
}
