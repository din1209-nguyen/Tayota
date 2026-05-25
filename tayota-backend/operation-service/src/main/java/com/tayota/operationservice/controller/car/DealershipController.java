package com.tayota.operationservice.controller.car;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.response.car.DealershipResponseDTO;
import com.tayota.operationservice.service.car.DealershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"", "/car"})
public class DealershipController {
    private final DealershipService dealershipService;

    @GetMapping("/dealerships")
    public ApiResponse<List<DealershipResponseDTO>> getActiveDealerships() {
        List<DealershipResponseDTO> result = dealershipService.getActiveDealerships();
        return ApiResponse.success(200, "Lấy danh sách đại lý thành công.", result);
    }
}
