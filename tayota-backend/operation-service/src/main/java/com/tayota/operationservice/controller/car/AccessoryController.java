package com.tayota.operationservice.controller.car;

import com.tayota.operationservice.dto.request.car.AccessoryRequestDTO;
import com.tayota.operationservice.dto.request.car.CarAccessoryRequestDTO;
import com.tayota.operationservice.dto.response.car.AccessoryResponseDTO;
import com.tayota.operationservice.dto.response.car.PaginationResponseDTO;
import com.tayota.operationservice.service.car.AccessoryService;
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

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/accessories")
public class AccessoryController {
    private final AccessoryService accessoryService;

    // Lấy tất cả phụ kiện
    @GetMapping
    public ApiResponse<PaginationResponseDTO<AccessoryResponseDTO>> searchAccessories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String seriesId,
            @RequestParam(required = false) String versionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PaginationResponseDTO<AccessoryResponseDTO> result = accessoryService.searchAccessories(
                keyword, type, seriesId, versionId, page, size
        );
        return ApiResponse.success(200, "Lấy danh sách phụ kiện thành công.", result);
    }

    // Lấy phụ kiện theo id
    @GetMapping("/{accessoryId}")
    public ApiResponse<AccessoryResponseDTO> getAccessory(@PathVariable String accessoryId) {
        AccessoryResponseDTO result = accessoryService.getAccessory(accessoryId);
        return ApiResponse.success(200, "Lấy phụ kiện thành công.", result);
    }

    // Thêm phụ kiện
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PostMapping
    public ApiResponse<AccessoryResponseDTO> createAccessory(@Valid @RequestBody AccessoryRequestDTO requestDTO) {
        AccessoryResponseDTO result = accessoryService.createAccessory(requestDTO);
        return ApiResponse.success(201, "Thêm phụ kiện thành công.", result);
    }

    // Cập nhật phụ kiện
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PutMapping("/{accessoryId}")
    public ApiResponse<AccessoryResponseDTO> updateAccessory(
            @PathVariable String accessoryId,
            @Valid @RequestBody AccessoryRequestDTO requestDTO
    ) {
        AccessoryResponseDTO result = accessoryService.updateAccessory(accessoryId, requestDTO);
        return ApiResponse.success(200, "Cập nhật phụ kiện thành công.", result);
    }

    // Xóa phụ kiện
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @DeleteMapping("/{accessoryId}")
    public ApiResponse<Void> deleteAccessory(@PathVariable String accessoryId) {
        accessoryService.deleteAccessory(accessoryId);
        return ApiResponse.success(200, "Xóa phụ kiện thành công.", null);
    }

    // Gắn phụ kiện cho phiên bản xe
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PostMapping("/car-versions")
    public ApiResponse<Void> attachAccessoryToVersion(@Valid @RequestBody CarAccessoryRequestDTO requestDTO) {
        accessoryService.attachAccessoryToVersion(requestDTO);
        return ApiResponse.success(200, "Gắn phụ kiện cho phiên bản xe thành công.", null);
    }

    // Gỡ phụ kiện khỏi phiên bản xe
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @DeleteMapping("/car-versions")
    public ApiResponse<Void> detachAccessoryFromVersion(@Valid @RequestBody CarAccessoryRequestDTO requestDTO) {
        accessoryService.detachAccessoryFromVersion(requestDTO);
        return ApiResponse.success(200, "Gỡ phụ kiện khỏi phiên bản xe thành công.", null);
    }
}
