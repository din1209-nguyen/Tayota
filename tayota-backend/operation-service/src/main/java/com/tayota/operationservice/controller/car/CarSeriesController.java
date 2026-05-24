package com.tayota.operationservice.controller.car;

import com.tayota.operationservice.dto.request.car.CarSeriesRequestDTO;
import com.tayota.operationservice.dto.response.car.CarSeriesResponseDTO;
import com.tayota.operationservice.service.car.CarSeriesService;
import com.tayota.operationservice.dto.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/car-series")
public class CarSeriesController {
    private final CarSeriesService carSeriesService;

    // Lấy danh sách dòng xe
    @GetMapping
    public ApiResponse<List<CarSeriesResponseDTO>> getCarSeries(@RequestParam(required = false) String carStyleId) {
        List<CarSeriesResponseDTO> result = carSeriesService.getCarSeries(carStyleId);
        return ApiResponse.success(200, "Lấy danh sách dòng xe thành công.", result);
    }

    // Lấy dòng xe theo id
    @GetMapping("/{carSeriesId}")
    public ApiResponse<CarSeriesResponseDTO> getCarSeriesById(@PathVariable String carSeriesId) {
        CarSeriesResponseDTO result = carSeriesService.getCarSeriesById(carSeriesId);
        return ApiResponse.success(200, "Lấy dòng xe thành công.", result);
    }

    // Thêm dòng xe
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<CarSeriesResponseDTO> createCarSeries(@Valid @RequestBody CarSeriesRequestDTO requestDTO) {
        CarSeriesResponseDTO result = carSeriesService.createCarSeries(requestDTO);
        return ApiResponse.success(201, "Thêm dòng xe thành công.", result);
    }

    // Cập nhật dòng xe
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{carSeriesId}")
    public ApiResponse<CarSeriesResponseDTO> updateCarSeries(
            @PathVariable String carSeriesId,
            @Valid @RequestBody CarSeriesRequestDTO requestDTO
    ) {
        CarSeriesResponseDTO result = carSeriesService.updateCarSeries(carSeriesId, requestDTO);
        return ApiResponse.success(200, "Cập nhật dòng xe thành công.", result);
    }

    // Xóa dòng xe
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{carSeriesId}")
    public ApiResponse<Void> deleteCarSeries(@PathVariable String carSeriesId) {
        carSeriesService.deleteCarSeries(carSeriesId);
        return ApiResponse.success(200, "Xóa dòng xe thành công.", null);
    }
}
