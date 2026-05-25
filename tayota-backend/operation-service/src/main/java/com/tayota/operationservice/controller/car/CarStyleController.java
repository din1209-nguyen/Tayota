package com.tayota.operationservice.controller.car;

import com.tayota.operationservice.dto.request.car.CarStyleRequestDTO;
import com.tayota.operationservice.dto.response.car.CarStyleResponseDTO;
import com.tayota.operationservice.service.car.CarStyleService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/car-styles")
public class CarStyleController {
    private final CarStyleService carStyleService;

    // Lấy danh sách kiểu xe
    @GetMapping
    public ApiResponse<List<CarStyleResponseDTO>> getCarStyles() {
        List<CarStyleResponseDTO> carStyles = carStyleService.getCarStyles();
        return ApiResponse.success(200, "Lấy danh sách kiểu xe thành công.", carStyles);
    }

    // Lấy kiểu xe theo id
    @GetMapping("/{carStyleId}")
    public ApiResponse<CarStyleResponseDTO> getCarStyle(@PathVariable String carStyleId) {
        CarStyleResponseDTO carStyle = carStyleService.getCarStyle(carStyleId);
        return ApiResponse.success(200, "Lấy kiểu xe thành công.", carStyle);
    }

    // Thêm kiểu xe
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PostMapping
    public ApiResponse<CarStyleResponseDTO> createCarStyle(@Valid @RequestBody CarStyleRequestDTO requestDTO) {
        CarStyleResponseDTO carStyle = carStyleService.createCarStyle(requestDTO);
        return ApiResponse.success(201, "Thêm kiểu xe thành công.", carStyle);
    }

    // Cập nhật kiểu xe
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PutMapping("/{carStyleId}")
    public ApiResponse<CarStyleResponseDTO> updateCarStyle(
            @PathVariable String carStyleId,
            @Valid @RequestBody CarStyleRequestDTO requestDTO
    ) {
        CarStyleResponseDTO carStyle = carStyleService.updateCarStyle(carStyleId, requestDTO);
        return ApiResponse.success(200, "Cập nhật kiểu xe thành công.", carStyle);
    }

    // Xóa kiểu xe
    @DeleteMapping("/{carStyleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCarStyle(@PathVariable String carStyleId) {
        carStyleService.deleteCarStyle(carStyleId);
        return ApiResponse.success(200, "Xóa kiểu xe thành công.", null);
    }
}
