package com.tayota.carservice.controller;

import com.tayota.carservice.dto.Request.CarRequestDTO;
import com.tayota.carservice.dto.Response.CarResponseDTO;
import com.tayota.carservice.dto.Response.PaginationResponseDTO;
import com.tayota.carservice.enums.CarStatusType;
import com.tayota.carservice.service.CarManagementService;
import com.tayota.commoncore.dto.ApiResponse;
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
@RequestMapping("/cars")
public class CarManagementController {
    private final CarManagementService carManagementService;

    // Lấy danh sách xe vật lý
    @GetMapping
    public ApiResponse<PaginationResponseDTO<CarResponseDTO>> searchCars(
            @RequestParam(required = false) String carVersionId,
            @RequestParam(required = false) String dealershipId,
            @RequestParam(required = false) String ownerUserId,
            @RequestParam(required = false) CarStatusType status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PaginationResponseDTO<CarResponseDTO> result = carManagementService.searchCars(
                carVersionId, dealershipId, ownerUserId, status, page, size
        );
        return ApiResponse.success(200, "Lấy danh sách xe vật lý thành công.", result);
    }

    // Lấy các xe sở hữu theo userId
    @GetMapping("/owner/{userId}")
    public ApiResponse<List<CarResponseDTO>> getCarsByUserId(@PathVariable String userId) {
        List<CarResponseDTO> result = carManagementService.getCarsByUserId(userId);
        return ApiResponse.success(200, "Lấy danh sách xe sở hữu thành công.", result);
    }

    // Lấy xe vật lý theo VIN
    @GetMapping("/{vinId}")
    public ApiResponse<CarResponseDTO> getCar(@PathVariable String vinId) {
        CarResponseDTO result = carManagementService.getCar(vinId);
        return ApiResponse.success(200, "Lấy xe vật lý thành công.", result);
    }

    // Thêm xe vật lý
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<CarResponseDTO> createCar(@Valid @RequestBody CarRequestDTO requestDTO) {
        CarResponseDTO result = carManagementService.createCar(requestDTO);
        return ApiResponse.success(201, "Thêm xe vật lý thành công.", result);
    }

    // Cập nhật xe vật lý
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{vinId}")
    public ApiResponse<CarResponseDTO> updateCar(
            @PathVariable String vinId,
            @Valid @RequestBody CarRequestDTO requestDTO
    ) {
        CarResponseDTO result = carManagementService.updateCar(vinId, requestDTO);
        return ApiResponse.success(200, "Cập nhật xe vật lý thành công.", result);
    }

    // Xóa xe vật lý
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{vinId}")
    public ApiResponse<Void> deleteCar(@PathVariable String vinId) {
        carManagementService.deleteCar(vinId);
        return ApiResponse.success(200, "Xóa xe vật lý thành công.", null);
    }
}
