package com.tayota.operationservice.controller.car;

import com.tayota.operationservice.dto.common.ApiResponse;
import com.tayota.operationservice.dto.response.car.DealershipResponseDTO;
import com.tayota.operationservice.entity.car.Dealership;
import com.tayota.operationservice.repository.car.DealershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"", "/car"})
public class DealershipController {
    private final DealershipRepository dealershipRepository;

    @GetMapping("/dealerships")
    public ApiResponse<List<DealershipResponseDTO>> getActiveDealerships() {
        List<DealershipResponseDTO> result = dealershipRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.success(200, "Lấy danh sách đại lý thành công.", result);
    }

    private DealershipResponseDTO toResponse(Dealership dealership) {
        return new DealershipResponseDTO(
                dealership.getId(),
                dealership.getName(),
                dealership.getAddress(),
                dealership.getPhone(),
                dealership.getOperatingHours(),
                dealership.getLatitude(),
                dealership.getLongitude()
        );
    }
}
