package com.tayota.operationservice.controller.car;

import com.tayota.operationservice.dto.request.car.CarSpecificationRequestDTO;
import com.tayota.operationservice.dto.request.car.CarVersionRequestDTO;
import com.tayota.operationservice.dto.request.car.CarPriceRequestDTO;
import com.tayota.operationservice.dto.request.car.CarGalleryRequestDTO;
import com.tayota.operationservice.dto.response.car.CarSpecificationResponseDTO;
import com.tayota.operationservice.dto.response.car.CarVersionItemResponseDTO;
import com.tayota.operationservice.dto.response.car.CarPriceResponseDTO;
import com.tayota.operationservice.dto.response.car.CarGalleryResponseDTO;
import com.tayota.operationservice.service.car.CarVersionService;
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
@RequestMapping("/car-versions")
public class CarVersionController {
    private final CarVersionService carVersionService;

    // Lấy danh sách phiên bản xe
    @GetMapping
    public ApiResponse<List<CarVersionItemResponseDTO>> getCarVersions(@RequestParam(required = false) String carSeriesId) {
        List<CarVersionItemResponseDTO> result = carVersionService.getCarVersions(carSeriesId);
        return ApiResponse.success(200, "Lấy danh sách phiên bản xe thành công.", result);
    }

    // Thêm phiên bản xe
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PostMapping
    public ApiResponse<CarVersionItemResponseDTO> createCarVersion(@Valid @RequestBody CarVersionRequestDTO requestDTO) {
        CarVersionItemResponseDTO result = carVersionService.createCarVersion(requestDTO);
        return ApiResponse.success(201, "Thêm phiên bản xe thành công.", result);
    }

    // Cập nhật phiên bản xe
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PutMapping("/{carVersionId}")
    public ApiResponse<CarVersionItemResponseDTO> updateCarVersion(
            @PathVariable String carVersionId,
            @Valid @RequestBody CarVersionRequestDTO requestDTO
    ) {
        CarVersionItemResponseDTO result = carVersionService.updateCarVersion(carVersionId, requestDTO);
        return ApiResponse.success(200, "Cập nhật phiên bản xe thành công.", result);
    }

    // Xóa phiên bản xe
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @DeleteMapping("/{carVersionId}")
    public ApiResponse<Void> deleteCarVersion(@PathVariable String carVersionId) {
        carVersionService.deleteCarVersion(carVersionId);
        return ApiResponse.success(200, "Xóa phiên bản xe thành công.", null);
    }

    // Lưu thông số kỹ thuật của xe
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PutMapping("/{carVersionId}/specification")
    public ApiResponse<CarSpecificationResponseDTO> saveSpecification(
            @PathVariable String carVersionId,
            @Valid @RequestBody CarSpecificationRequestDTO requestDTO
    ) {
        CarSpecificationResponseDTO result = carVersionService.saveSpecification(carVersionId, requestDTO);
        return ApiResponse.success(200, "Lưu thông số kỹ thuật xe thành công.", result);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PutMapping("/{carVersionId}/prices")
    public ApiResponse<CarPriceResponseDTO> savePrice(
            @PathVariable String carVersionId, @Valid @RequestBody CarPriceRequestDTO requestDTO) {
        return ApiResponse.success(200, "Lưu giá xe thành công.", carVersionService.savePrice(carVersionId, requestDTO));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @DeleteMapping("/{carVersionId}/prices")
    public ApiResponse<Void> deletePrice(
            @PathVariable String carVersionId,
            @RequestParam String exteriorColorId,
            @RequestParam String interiorColorId) {
        carVersionService.deletePrice(carVersionId, exteriorColorId, interiorColorId);
        return ApiResponse.success(200, "Xóa cấu hình giá thành công.", null);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PostMapping("/{carVersionId}/galleries")
    public ApiResponse<CarGalleryResponseDTO> addGallery(
            @PathVariable String carVersionId, @Valid @RequestBody CarGalleryRequestDTO requestDTO) {
        return ApiResponse.success(201, "Thêm ảnh thành công.", carVersionService.addGallery(carVersionId, requestDTO));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PutMapping("/{carVersionId}/galleries/{galleryId}")
    public ApiResponse<CarGalleryResponseDTO> updateGallery(
            @PathVariable String carVersionId, @PathVariable String galleryId,
            @Valid @RequestBody CarGalleryRequestDTO requestDTO) {
        return ApiResponse.success(200, "Cập nhật ảnh thành công.", carVersionService.updateGallery(carVersionId, galleryId, requestDTO));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @DeleteMapping("/{carVersionId}/galleries/{galleryId}")
    public ApiResponse<Void> deleteGallery(@PathVariable String carVersionId, @PathVariable String galleryId) {
        carVersionService.deleteGallery(carVersionId, galleryId);
        return ApiResponse.success(200, "Xóa ảnh thành công.", null);
    }
}
