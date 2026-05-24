package com.tayota.operationservice.car.controller;

import com.tayota.operationservice.car.dto.Response.CarSpecificationResponseDTO;
import com.tayota.operationservice.car.dto.Response.CarStyleWithVersionsResponseDTO;
import com.tayota.operationservice.car.dto.Response.CarVersionDetailResponseDTO;
import com.tayota.operationservice.car.dto.Response.CarVersionItemResponseDTO;
import com.tayota.operationservice.car.dto.Response.PaginationResponseDTO;
import com.tayota.operationservice.car.service.CarCatalogService;
import com.tayota.commoncore.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CarCatalogController {
    private final CarCatalogService carCatalogService;

    // Lấy danh sách tất cả phiên bản xe và kiểu dáng
    @GetMapping("/catalog/car-styles-with-versions")
    public ApiResponse<List<CarStyleWithVersionsResponseDTO>> getStylesWithVersions() {
        List<CarStyleWithVersionsResponseDTO> result = carCatalogService.getStylesWithVersions();
        return ApiResponse.success(200, "Lấy danh sách kiểu dáng và phiên bản xe thành công.", result);
    }

    // Lấy tất cả danh sách xe theo điều kiện lọc
    @GetMapping("/catalog/car-versions")
    public ApiResponse<PaginationResponseDTO<CarVersionItemResponseDTO>> searchCarVersions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String styleId,
            @RequestParam(required = false) String seriesId,
            @RequestParam(required = false) Integer modelYear,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PaginationResponseDTO<CarVersionItemResponseDTO> result = carCatalogService.searchCarVersions(
                keyword, styleId, seriesId, modelYear, minPrice, maxPrice, page, size
        );
        return ApiResponse.success(200, "Lấy danh sách phiên bản xe thành công.", result);
    }

    // Lấy tất cả thông tin xe cụ thể
    @GetMapping("/catalog/car-versions/{carVersionId}")
    public ApiResponse<CarVersionDetailResponseDTO> getCarVersionDetail(@PathVariable String carVersionId) {
        CarVersionDetailResponseDTO result = carCatalogService.getCarVersionDetail(carVersionId);
        return ApiResponse.success(200, "Lấy thông tin xe thành công.", result);
    }

    // Xem thông số kỹ thuật của xe
    @GetMapping("/catalog/car-versions/{carVersionId}/specification")
    public ApiResponse<CarSpecificationResponseDTO> getCarSpecification(@PathVariable String carVersionId) {
        CarSpecificationResponseDTO result = carCatalogService.getCarSpecification(carVersionId);
        return ApiResponse.success(200, "Lấy thông số kỹ thuật xe thành công.", result);
    }

    // So sánh xe
    @GetMapping("/catalog/car-versions/compare")
    public ApiResponse<List<CarVersionDetailResponseDTO>> compareCarVersions(@RequestParam List<String> ids) {
        List<CarVersionDetailResponseDTO> result = carCatalogService.compareCarVersions(ids);
        return ApiResponse.success(200, "So sánh xe thành công.", result);
    }
}
